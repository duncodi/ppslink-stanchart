package com.duncodi.ppslink.stanchart.service;

import com.duncodi.ppslink.stanchart.dto.StraightToBankConfigDto;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SftpFileUploadService {

    private static final int SESSION_TIMEOUT = 100_000;
    private static final int CHANNEL_TIMEOUT = 100_000;

    private Session createSession(StraightToBankConfigDto config) throws JSchException {

        JSch jsch = new JSch();

       /* if (config.getSftpKnownHostsPath() != null) {
            jsch.setKnownHosts(config.getSftpKnownHostsPath());
        }

        if (config.getSftpPrivateKeyPath() != null) {
            if (config.getSftpPassword() != null && !config.getSftpPassword().isEmpty()) {
                jsch.addIdentity(config.getSftpPrivateKeyPath(), config.getSftpPassword());
            } else {
                jsch.addIdentity(config.getSftpPrivateKeyPath());
            }
        }

        Session session = jsch.getSession(
                config.getSftpUsername(),
                config.getSftpRemoteHost(),
                config.getSftpRemotePort()
        );

        if (config.getSftpPassword() != null && config.getSftpPrivateKeyPath() == null) {
            session.setPassword(config.getSftpPassword());
        }

        Properties configProps = new Properties();
        configProps.put("StrictHostKeyChecking", "no");
        session.setConfig(configProps);

        session.connect(SESSION_TIMEOUT);

        return session;*/

        return null;

    }

    private ChannelSftp openSftpChannel(Session session) throws JSchException {
        com.jcraft.jsch.Channel channel = session.openChannel("sftp");
        channel.connect(CHANNEL_TIMEOUT);
        return (ChannelSftp) channel;
    }

    public void getFile(StraightToBankConfigDto config, String remotePath, String localPath, boolean removeAfterGet) throws Exception {

        Session session = null;

        ChannelSftp channelSftp = null;

        try {
            session = createSession(config);
            channelSftp = openSftpChannel(session);

            log.info("Downloading [{}] → [{}]", remotePath, localPath);
            channelSftp.get(remotePath, localPath);

            if (removeAfterGet) {
                channelSftp.rm(remotePath);
                log.info("Removed remote file: {}", remotePath);
            }

        } catch (Exception e) {
            log.error("❌ SFTP download failed: {}", e.getMessage(), e);
            e.printStackTrace(System.err);
            throw new Exception(e.getMessage());
        } finally {

            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.exit();
            }

            if (session != null && session.isConnected()) {
                session.disconnect();
            }

            log.info("🔌 SFTP session closed");

        }

    }

    public List<ChannelSftp.LsEntry> listAllFiles(StraightToBankConfigDto config, String remoteDirectory) throws Exception {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            session = createSession(config);
            channelSftp = openSftpChannel(session);

            return (List<ChannelSftp.LsEntry>) channelSftp.ls(remoteDirectory);

        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.exit();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

}
