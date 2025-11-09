package com.duncodi.ppslink.stanchart.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.Security;

@RequiredArgsConstructor
@Slf4j
@Service
public class SshAndPgpEncryptionService {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // ========================= ENCRYPTION =========================

    public void encrypt(String fileName, OutputStream out, PGPPublicKey encKey) throws Exception {
        try (OutputStream armoredOut = new ArmoredOutputStream(out);
             ByteArrayOutputStream bOut = new ByteArrayOutputStream()) {

            log.info("Compressing input file...");

            PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(PGPCompressedDataGenerator.ZIP);
            try (OutputStream cos = comData.open(bOut)) {
                PGPUtil.writeFileToLiteralData(cos, PGPLiteralData.BINARY, new File(fileName));
            }
            comData.close();

            log.info("Creating PGP encrypted data generator...");

            JcePGPDataEncryptorBuilder dataEncryptor = new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                    .setWithIntegrityPacket(true)
                    .setSecureRandom(new SecureRandom())
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);

            PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(dataEncryptor);

            cPk.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .setSecureRandom(new SecureRandom()));

            byte[] bytes = bOut.toByteArray();
            try (OutputStream cOut = cPk.open(armoredOut, bytes.length)) {
                cOut.write(bytes);
            }

            log.info("Encryption successful for file: {}", fileName);
        }
    }

    // ========================= GPG USING LOCAL SHELL =========================

    public void processEncryptionUsingLinux(String plainFolderPath, String fileNameNative, String pgpUser, String localPgpKeyPassword,
                                            String encryptedFolderPath, String sshIpAddress, String sshUser, String sshPassword) throws Exception {

        Session session = null;

        try {

            session = this.connectToRemote(sshIpAddress, sshUser, sshPassword);

            //Run remote encryption
            this.processEncryptionUsingLinuxRemote(session, plainFolderPath, fileNameNative, pgpUser, localPgpKeyPassword,
                    encryptedFolderPath);

        } finally {
            if (session != null && session.isConnected()){
                session.disconnect();
            }
        }

    }

    public void processEncryptionUsingLinuxRemote(Session session,
                                                  String plainFolderPath,
                                                  String fileNameNative,
                                                  String pgpUser,
                                                  String localPgpKeyPassword,
                                                  String encryptedFolderPath) throws Exception {


        String filePath = plainFolderPath + "/" + fileNameNative;

        String cmd = "gpg --encrypt --sign --armor --always-trust --pinentry-mode=loopback " +
                "--passphrase=" + localPgpKeyPassword + " -r " + pgpUser + " " + filePath;

        this.runRemoteCmd(session, cmd);

        String encryptedFileNow = filePath + ".asc";
        String newFileInEncryptedFolderPath = encryptedFolderPath + "/" + fileNameNative;

        // On remote machine, move/copy file using shell

        this.runRemoteCmd(session, "cp " + encryptedFileNow + " " + newFileInEncryptedFolderPath);

        log.info("File encrypted successfully on remote server using Linux GPG: {}", newFileInEncryptedFolderPath);

    }

    public void processEncryptionUsingLinuxLocal(String plainFolderPath, String fileNameNative,
                                            String pgpUser, String localPgpKeyPassword,
                                            String encryptedFolderPath) throws Exception {

        String filePath = plainFolderPath + "/" + fileNameNative;

        String cmd = "gpg --encrypt --sign --armor --always-trust --pinentry-mode=loopback " +
                "--passphrase=" + localPgpKeyPassword + " -r " + pgpUser + " " + filePath;

        this.runCmd(cmd);

        String encryptedFileNow = filePath + ".asc";
        File file = new File(encryptedFileNow);

        String newFileInEncryptedFolderPath = encryptedFolderPath + "/" + fileNameNative;
        FileUtils.copyFile(file, new File(newFileInEncryptedFolderPath));

        log.info("File encrypted successfully using Linux GPG: {}", newFileInEncryptedFolderPath);
    }

    public void processDecryptionUsingLinux(String plainFolderPath, String localPgpKeyPassword,
                                            String encryptedFolderPath, String filePath,
                                            boolean encrypted) throws Exception {

        String fileNameNative = filePath.replace(encryptedFolderPath, "").replace("/", "");
        String destinationPath = plainFolderPath + "/" + fileNameNative;

        if (encrypted) {
            String cmd = "gpg --pinentry-mode=loopback --passphrase=" + localPgpKeyPassword +
                    " --output " + destinationPath + " --decrypt " + filePath;

            log.info("DECRYPTION CMD::::::::{}", cmd);
            this.runCmd(cmd);
        } else {
            Files.copy(Paths.get(filePath), Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ========================= SSH SUPPORT =========================

    public Session connectToRemote(String ipAddress, String user, String password) throws Exception {

        JSch jsch = new JSch();
        Session session = jsch.getSession(user, ipAddress, 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30000);
        log.info("SSH session connected to {}@{}", user, ipAddress);

        return session;

    }

    public void sshIntoFilesServer(String ipAddress, String user, String password) throws Exception {
        Session session = null;
        try {
            session = connectToRemote(ipAddress, user, password);
            runRemoteCmd(session, "ls -la"); // Example command
        } finally {
            if (session != null && session.isConnected()) session.disconnect();
        }
    }

    private String runRemoteCmd(Session session, String cmd) throws Exception {

        ChannelExec channel = null;

        try {
            log.info("Running remote command: {}", cmd);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            channel.setInputStream(null);

            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

            channel.setOutputStream(stdoutStream);
            channel.setErrStream(stderrStream);

            channel.connect();

            // Wait for command to finish
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            int exitStatus = channel.getExitStatus();

            String stdout = stdoutStream.toString().trim();
            String stderr = stderrStream.toString().trim();

            log.info("Remote CMD STDOUT: {}", stdout);
            if (!stderr.isEmpty()) {
                log.warn("Remote CMD STDERR: {}", stderr);
            }
            log.info("Remote CMD exit status: {}", exitStatus);

            if (exitStatus != 0) {
                throw new Exception("Remote command failed with exit code "
                        + exitStatus + ": " + stderr);
            }

            return stdout;

        } catch (Exception e) {
            log.error("Remote command execution failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    // ========================= LOCAL CMD RUNNER =========================

    private void runCmd(String cmd) throws Exception {
        String[] command = {"bash", "-c", cmd};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> log.info("CMD OUT: {}", line));
        }

        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + cmd);
        }
    }

    // ========================= KEY HELPERS =========================

    public static PGPPublicKey readPublicKey(String keyPath) throws IOException, PGPException {
        try (InputStream keyIn = PGPUtil.getDecoderStream(new FileInputStream(keyPath))) {
            PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(keyIn, new JcaKeyFingerprintCalculator());
            for (PGPPublicKeyRing keyRing : pgpPub) {
                for (PGPPublicKey key : keyRing) {
                    if (key.isEncryptionKey()) {
                        return key;
                    }
                }
            }
            throw new IllegalArgumentException("No encryption key found in public keyring.");
        }
    }

    public static PGPSecretKey readPrivateKey(String keyPath) throws IOException, PGPException {
        try (InputStream keyIn = PGPUtil.getDecoderStream(new FileInputStream(keyPath))) {
            PGPSecretKeyRingCollection pgpPriv = new PGPSecretKeyRingCollection(keyIn, new JcaKeyFingerprintCalculator());
            for (PGPSecretKeyRing keyRing : pgpPriv) {
                for (PGPSecretKey key : keyRing) {
                    if (key.isSigningKey()) {
                        return key;
                    }
                }
            }
            throw new IllegalArgumentException("No signing key found in secret keyring.");
        }
    }
}
