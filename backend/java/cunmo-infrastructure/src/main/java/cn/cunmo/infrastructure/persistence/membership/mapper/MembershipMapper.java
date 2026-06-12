package cn.cunmo.infrastructure.persistence.membership.mapper;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 会员上下文 MyBatis Mapper。
 */
public interface MembershipMapper {
    @Select("""
            SELECT user_id AS userId, plan_type AS planType,
                   effective_at AS effectiveAt, expires_at AS expiresAt,
                   source_reference AS sourceReference
            FROM membership_entitlement
            WHERE user_id = #{userId}
            """)
    EntitlementRow selectEntitlement(@Param("userId") long userId);

    @Insert("""
            INSERT INTO membership_entitlement (
                user_id, plan_type, effective_at, expires_at,
                source_reference
            ) VALUES (
                #{userId}, #{planType}, #{effectiveAt}, #{expiresAt},
                #{sourceReference}
            )
            ON DUPLICATE KEY UPDATE
                plan_type = VALUES(plan_type),
                effective_at = VALUES(effective_at),
                expires_at = VALUES(expires_at),
                source_reference = VALUES(source_reference),
                version = version + 1
            """)
    int upsertEntitlement(
            @Param("userId") long userId,
            @Param("planType") String planType,
            @Param("effectiveAt") Instant effectiveAt,
            @Param("expiresAt") Instant expiresAt,
            @Param("sourceReference") String sourceReference);

    @Select("""
            SELECT identity.openid
            FROM sys_wechat_identity identity
            JOIN sys_user user_table ON user_table.id = identity.user_id
            WHERE identity.user_id = #{userId}
              AND user_table.status = 1 AND user_table.deleted = 0
            LIMIT 1
            """)
    String selectWechatOpenId(@Param("userId") long userId);

    @Insert("""
            INSERT INTO membership_order (
                order_no, user_id, product_code, amount_fen,
                auto_renew, status
            ) VALUES (
                #{orderNo}, #{userId}, #{productCode}, #{amountFen},
                #{autoRenew}, 'CREATED'
            )
            """)
    int insertOrder(
            @Param("orderNo") String orderNo,
            @Param("userId") long userId,
            @Param("productCode") String productCode,
            @Param("amountFen") int amountFen,
            @Param("autoRenew") boolean autoRenew);

    @Update("""
            UPDATE membership_order
            SET status = 'PAYING', prepay_id = #{prepayId}
            WHERE order_no = #{orderNo} AND status = 'CREATED'
            """)
    int markOrderPaying(
            @Param("orderNo") String orderNo,
            @Param("prepayId") String prepayId);

    @Select("""
            SELECT order_no AS orderNo, user_id AS userId,
                   product_code AS productCode, amount_fen AS amountFen,
                   auto_renew AS autoRenew, status, prepay_id AS prepayId
            FROM membership_order
            WHERE user_id = #{userId} AND order_no = #{orderNo}
            """)
    OrderRow selectOwnedOrder(
            @Param("userId") long userId,
            @Param("orderNo") String orderNo);

    @Select("""
            SELECT order_no AS orderNo, user_id AS userId,
                   product_code AS productCode, amount_fen AS amountFen,
                   auto_renew AS autoRenew, status, prepay_id AS prepayId
            FROM membership_order
            WHERE order_no = #{orderNo}
            FOR UPDATE
            """)
    OrderRow selectOrderForUpdate(@Param("orderNo") String orderNo);

    @Update("""
            UPDATE membership_order
            SET status = 'PAID',
                wechat_transaction_id = #{transactionId},
                paid_at = #{paidAt}
            WHERE order_no = #{orderNo} AND status <> 'PAID'
            """)
    int markOrderPaid(
            @Param("orderNo") String orderNo,
            @Param("transactionId") String transactionId,
            @Param("paidAt") Instant paidAt);

    @Insert("""
            INSERT IGNORE INTO membership_payment_notification (
                notification_id, notification_type, business_reference
            ) VALUES (
                #{notificationId}, #{type}, #{businessReference}
            )
            """)
    int insertNotification(
            @Param("notificationId") String notificationId,
            @Param("type") String type,
            @Param("businessReference") String businessReference);

    @Select("""
            SELECT COUNT(*)
            FROM inv_space space
            JOIN inv_vault vault ON vault.id = space.vault_id
            WHERE vault.owner_user_id = #{userId}
              AND space.status = 1 AND space.deleted = 0
            """)
    int countRootSpaces(@Param("userId") long userId);

    @Select("""
            SELECT space.id AS spaceId, space.space_name AS spaceName,
                   COUNT(binding.category_id) AS categoryCount
            FROM inv_vault vault
            JOIN inv_space space ON space.vault_id = vault.id
              AND space.status = 1 AND space.deleted = 0
            LEFT JOIN inv_space_category binding
              ON binding.vault_id = vault.id AND binding.space_id = space.id
            WHERE vault.owner_user_id = #{userId}
            GROUP BY space.id, space.space_name, space.sort_order
            ORDER BY space.sort_order, space.id
            """)
    List<SpaceQuotaRow> selectSpaceQuotas(
            @Param("userId") long userId);

    @Select("""
            SELECT space.id AS spaceId, space.space_name AS spaceName,
                   category.id AS categoryId,
                   category.category_name AS categoryName,
                   COUNT(item.id) AS itemCount
            FROM inv_vault vault
            JOIN inv_space space ON space.vault_id = vault.id
              AND space.status = 1 AND space.deleted = 0
            JOIN inv_space_category binding
              ON binding.vault_id = vault.id AND binding.space_id = space.id
            JOIN inv_category category ON category.id = binding.category_id
              AND category.status = 1 AND category.deleted = 0
            LEFT JOIN inv_item item ON item.vault_id = vault.id
              AND item.space_id = space.id
              AND item.category_id = category.id
              AND item.status = 1 AND item.deleted = 0
            WHERE vault.owner_user_id = #{userId}
            GROUP BY space.id, space.space_name,
                     category.id, category.category_name
            ORDER BY itemCount DESC, space.id, category.id
            """)
    List<CapacityRow> selectCapacityUsages(
            @Param("userId") long userId);

    @Select("""
            SELECT id, user_id AS userId, status,
                   next_charge_at AS nextChargeAt,
                   contract_code AS contractCode,
                   wechat_contract_id AS wechatContractId
            FROM membership_renewal_agreement
            WHERE user_id = #{userId}
            """)
    RenewalRow selectRenewal(@Param("userId") long userId);

    @Select("""
            SELECT id, user_id AS userId, status,
                   next_charge_at AS nextChargeAt,
                   contract_code AS contractCode,
                   wechat_contract_id AS wechatContractId
            FROM membership_renewal_agreement
            WHERE contract_code = #{contractCode}
            """)
    RenewalRow selectRenewalByContractCode(
            @Param("contractCode") String contractCode);

    @Select("""
            SELECT id, user_id AS userId, status,
                   next_charge_at AS nextChargeAt,
                   contract_code AS contractCode,
                   wechat_contract_id AS wechatContractId
            FROM membership_renewal_agreement
            WHERE status = 'ACTIVE'
              AND next_charge_at <= #{dueAt}
            ORDER BY next_charge_at, id
            LIMIT #{limit}
            """)
    List<RenewalRow> selectDueRenewals(
            @Param("dueAt") Instant dueAt,
            @Param("limit") int limit);

    @Insert("""
            INSERT INTO membership_renewal_agreement (
                user_id, contract_code, status
            ) VALUES (
                #{userId}, #{contractCode}, 'PENDING'
            )
            ON DUPLICATE KEY UPDATE
                contract_code = VALUES(contract_code),
                wechat_contract_id = NULL,
                status = 'PENDING',
                next_charge_at = NULL,
                consecutive_failures = 0,
                terminated_at = NULL
            """)
    int savePendingRenewal(
            @Param("userId") long userId,
            @Param("contractCode") String contractCode);

    @Update("""
            UPDATE membership_renewal_agreement
            SET status = 'ACTIVE',
                wechat_contract_id = #{wechatContractId},
                next_charge_at = #{nextChargeAt}
            WHERE contract_code = #{contractCode}
            """)
    int activateRenewal(
            @Param("contractCode") String contractCode,
            @Param("wechatContractId") String wechatContractId,
            @Param("nextChargeAt") Instant nextChargeAt);

    @Update("""
            UPDATE membership_renewal_agreement
            SET status = 'TERMINATED',
                next_charge_at = NULL,
                terminated_at = #{terminatedAt}
            WHERE user_id = #{userId} AND status <> 'TERMINATED'
            """)
    int terminateRenewal(
            @Param("userId") long userId,
            @Param("terminatedAt") Instant terminatedAt);

    @Update("""
            UPDATE membership_renewal_agreement
            SET next_charge_at = #{nextChargeAt},
                consecutive_failures = 0
            WHERE user_id = #{userId} AND status = 'ACTIVE'
            """)
    int scheduleNextRenewal(
            @Param("userId") long userId,
            @Param("nextChargeAt") Instant nextChargeAt);

    @Insert("""
            INSERT INTO membership_renewal_attempt (
                attempt_no, agreement_id, amount_fen,
                scheduled_at, status
            ) VALUES (
                #{attemptNo}, #{agreementId}, #{amountFen},
                #{scheduledAt}, 'CREATED'
            )
            """)
    int insertRenewalAttempt(
            @Param("agreementId") long agreementId,
            @Param("attemptNo") String attemptNo,
            @Param("amountFen") int amountFen,
            @Param("scheduledAt") Instant scheduledAt);

    @Update("""
            UPDATE membership_renewal_attempt
            SET status = 'FAILED', failure_reason = #{reason}
            WHERE attempt_no = #{attemptNo}
            """)
    int failRenewalAttempt(
            @Param("attemptNo") String attemptNo,
            @Param("reason") String reason);

    @Update("""
            UPDATE membership_renewal_attempt
            SET status = 'PAID', charged_at = #{paidAt},
                wechat_transaction_id = #{transactionId},
                failure_reason = NULL
            WHERE attempt_no = #{attemptNo}
            """)
    int payRenewalAttempt(
            @Param("attemptNo") String attemptNo,
            @Param("transactionId") String transactionId,
            @Param("paidAt") Instant paidAt);

    @Update("""
            UPDATE membership_renewal_agreement
            SET consecutive_failures = consecutive_failures + 1,
                next_charge_at = CASE
                  WHEN consecutive_failures + 1 >= #{maxAttempts}
                    THEN DATE_ADD(next_charge_at, INTERVAL 30 DAY)
                  ELSE #{retryAt}
                END
            WHERE id = (
              SELECT agreement_id
              FROM membership_renewal_attempt
              WHERE attempt_no = #{attemptNo}
            )
            """)
    int recordRenewalFailure(
            @Param("attemptNo") String attemptNo,
            @Param("retryAt") Instant retryAt,
            @Param("maxAttempts") int maxAttempts);

    @Update("""
            UPDATE membership_entitlement
            SET plan_type = 'FREE',
                effective_at = NULL,
                expires_at = NULL,
                source_reference = 'EXPIRED',
                version = version + 1
            WHERE plan_type = 'MONTHLY_PRO'
              AND expires_at <= #{expiredAt}
            ORDER BY id
            LIMIT #{limit}
            """)
    int expireMonthlyEntitlements(
            @Param("expiredAt") Instant expiredAt,
            @Param("limit") int limit);

    @Select("""
            SELECT id, user_id AS userId, contact, remark, status,
                   grant_type AS grantType, grant_months AS grantMonths,
                   review_note AS reviewNote, created_at AS createdAt,
                   reviewed_at AS reviewedAt
            FROM membership_upgrade_request
            WHERE user_id = #{userId} AND status = 'PENDING'
            ORDER BY id DESC LIMIT 1
            """)
    UpgradeRow selectPendingUpgrade(@Param("userId") long userId);

    @Insert("""
            INSERT INTO membership_upgrade_request (
                user_id, contact, remark, status
            ) VALUES (
                #{userId}, #{contact}, #{remark}, 'PENDING'
            )
            """)
    int insertUpgrade(
            @Param("userId") long userId,
            @Param("contact") String contact,
            @Param("remark") String remark);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    @Select("""
            SELECT id, user_id AS userId, contact, remark, status,
                   grant_type AS grantType, grant_months AS grantMonths,
                   review_note AS reviewNote, created_at AS createdAt,
                   reviewed_at AS reviewedAt
            FROM membership_upgrade_request
            WHERE status = #{status}
            ORDER BY id DESC LIMIT #{limit}
            """)
    List<UpgradeRow> selectUpgrades(
            @Param("status") String status,
            @Param("limit") int limit);

    @Select("""
            SELECT id, user_id AS userId, contact, remark, status,
                   grant_type AS grantType, grant_months AS grantMonths,
                   review_note AS reviewNote, created_at AS createdAt,
                   reviewed_at AS reviewedAt
            FROM membership_upgrade_request
            WHERE id = #{requestId}
            FOR UPDATE
            """)
    UpgradeRow selectUpgradeForUpdate(
            @Param("requestId") long requestId);

    @Update("""
            UPDATE membership_upgrade_request
            SET status = 'APPROVED', reviewer_user_id = #{reviewerUserId},
                grant_type = #{grantType}, grant_months = #{months},
                review_note = #{note}, reviewed_at = #{reviewedAt}
            WHERE id = #{requestId} AND status = 'PENDING'
            """)
    int approveUpgrade(
            @Param("requestId") long requestId,
            @Param("reviewerUserId") long reviewerUserId,
            @Param("grantType") String grantType,
            @Param("months") Integer months,
            @Param("note") String note,
            @Param("reviewedAt") Instant reviewedAt);

    @Update("""
            UPDATE membership_upgrade_request
            SET status = 'REJECTED', reviewer_user_id = #{reviewerUserId},
                review_note = #{note}, reviewed_at = #{reviewedAt}
            WHERE id = #{requestId} AND status = 'PENDING'
            """)
    int rejectUpgrade(
            @Param("requestId") long requestId,
            @Param("reviewerUserId") long reviewerUserId,
            @Param("note") String note,
            @Param("reviewedAt") Instant reviewedAt);

    record EntitlementRow(
            long userId,
            String planType,
            Instant effectiveAt,
            Instant expiresAt,
            String sourceReference) {
    }

    record OrderRow(
            String orderNo,
            long userId,
            String productCode,
            int amountFen,
            boolean autoRenew,
            String status,
            String prepayId) {
    }

    record SpaceQuotaRow(
            long spaceId,
            String spaceName,
            int categoryCount) {
    }

    record CapacityRow(
            long spaceId,
            String spaceName,
            long categoryId,
            String categoryName,
            int itemCount) {
    }

    record RenewalRow(
            long id,
            long userId,
            String status,
            Instant nextChargeAt,
            String contractCode,
            String wechatContractId) {
    }

    record UpgradeRow(
            long id,
            long userId,
            String contact,
            String remark,
            String status,
            String grantType,
            Integer grantMonths,
            String reviewNote,
            Instant createdAt,
            Instant reviewedAt) {
    }
}
