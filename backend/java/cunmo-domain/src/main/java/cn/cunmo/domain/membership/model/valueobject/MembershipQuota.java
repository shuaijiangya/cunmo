package cn.cunmo.domain.membership.model.valueobject;

/**
 * 当前会员对应的库存配额。
 */
public record MembershipQuota(
        Integer rootSpaceLimit,
        Integer categoryLimitPerSpace,
        Integer itemLimitPerCavity) {
    public static MembershipQuota free() {
        return new MembershipQuota(3, 3, 10);
    }

    public static MembershipQuota unlimited() {
        return new MembershipQuota(null, null, null);
    }

    public boolean unlimitedQuota() {
        return rootSpaceLimit == null;
    }
}
