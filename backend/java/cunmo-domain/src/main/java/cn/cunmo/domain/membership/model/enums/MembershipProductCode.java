package cn.cunmo.domain.membership.model.enums;

/**
 * 可购买会员产品。
 */
public enum MembershipProductCode {
    MONTHLY_PRO(990, 30),
    LIFETIME_PRO(6900, null);

    private final int priceFen;
    private final Integer durationDays;

    MembershipProductCode(int priceFen, Integer durationDays) {
        this.priceFen = priceFen;
        this.durationDays = durationDays;
    }

    public int priceFen() {
        return priceFen;
    }

    public Integer durationDays() {
        return durationDays;
    }
}
