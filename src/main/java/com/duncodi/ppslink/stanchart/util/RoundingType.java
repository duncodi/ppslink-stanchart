package com.duncodi.ppslink.stanchart.util;
import java.math.BigDecimal;
import java.math.RoundingMode;


public enum RoundingType {

  NONE("None(No rounding)") {
    @Override
    public BigDecimal round(BigDecimal amount) {
      if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) return BigDecimal.ZERO;
      return amount;
    }
  },

  NEAREST_SHILLING("Nearest Shilling") {
    @Override
    public BigDecimal round(BigDecimal amount) {
      if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) return BigDecimal.ZERO;
      return amount.setScale(0, RoundingMode.HALF_UP);
    }
  },

  NEAREST_TEN_CENTS("Nearest ten cents") {
    @Override
    public BigDecimal round(BigDecimal amount) {
      if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) return BigDecimal.ZERO;
      return amount.setScale(1, RoundingMode.HALF_UP);
    }
  },

  NEAREST_FIVE_CENTS("Nearest five cents") {
    @Override
    public BigDecimal round(BigDecimal amount) {
      double nearest_cents = 20;
      BigDecimal number = new BigDecimal(String.valueOf(amount));
      BigDecimal floored =
          number.setScale(
              2,
              BigDecimal
                  .ROUND_DOWN); // Truncate up to 2 decimal places. *NOTE.* this isn't rounding off.
      number = new BigDecimal(Math.round(floored.doubleValue() * nearest_cents) / nearest_cents);
      number = number.setScale(2, RoundingMode.HALF_UP);
      return number;

      // if(amount == null || BigDecimal.ZERO.compareTo(amount) == 0) return BigDecimal.ZERO;
      // return new BigDecimal(((double) (long) (amount.doubleValue() * 20 + 0.5)) / 20);
    }
  },
  ROUND_DOWN("Round down") {
    @Override
    public BigDecimal round(BigDecimal amount) {
      if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) return BigDecimal.ZERO;
      return amount.setScale(0, RoundingMode.FLOOR);
    }
  },
  ROUND_UP("Round up") {
    @Override
    public BigDecimal round(BigDecimal amount) {
      if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) return BigDecimal.ZERO;
      return amount.setScale(0, RoundingMode.CEILING);
    }
  },
  NEAREST_FIFTY_CENTS("Nearest Fifty Cents") {
    @Override
    public BigDecimal round(BigDecimal amount) {
      if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) return BigDecimal.ZERO;
      return amount.setScale(0, RoundingMode.HALF_UP);
    }
  };

  private String name;

  RoundingType(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  public abstract BigDecimal round(BigDecimal round);
}
