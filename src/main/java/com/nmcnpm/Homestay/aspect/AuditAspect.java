package com.nmcnpm.Homestay.aspect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nmcnpm.Homestay.entity.Account;
import com.nmcnpm.Homestay.entity.AuditLog;
import com.nmcnpm.Homestay.enums.AccountRole;
import com.nmcnpm.Homestay.repository.AccountRepository;
import com.nmcnpm.Homestay.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

///**
// * AuditAspect — tự động ghi AuditLog cho các thao tác quan trọng.
// *
// * Cơ chế hoạt động:
// *  - Pointcut bám vào các method create*/update*/delete*/cancel*
// *    trong package service.
// *  - @AfterReturning: chỉ ghi log KHI method thành công (không ghi khi exception).
// *  - Lấy actor từ Spring SecurityContext.
// *  - Serialize result (response DTO) thành JSON để lưu vào new_data.
// *
// * Lưu ý:
// *  - old_data không được lấy tự động ở đây vì cần thêm một lần đọc DB trước khi
// *    update; nếu cần old_data chi tiết, có thể inject thêm logic ở từng service
// *    hoặc dùng Hibernate Envers. Hiện tại old_data = null — đủ cho MVP audit trail.
// *  - AuditLog được lưu trong một transaction riêng biệt (@Transactional REQUIRES_NEW
// *    ngầm định qua save) để không rollback cùng main transaction khi có lỗi logging.
// */
        @Slf4j
        @Aspect
        @Component
        @RequiredArgsConstructor
        public class AuditAspect {

            private final AuditLogRepository auditLogRepository;
            private final AccountRepository  accountRepository;
            private final ObjectMapper       objectMapper;

            // ------------------------------------------------------------------
            // Pointcut: create* trong service
            // ------------------------------------------------------------------
            @AfterReturning(
                    pointcut = "execution(* com.nmcnpm.Homestay.service.*Service.create*(..))",
                    returning = "result"
            )
            public void afterCreate(JoinPoint jp, Object result) {
                String action     = "CREATE";
                String entityName = resolveEntityName(jp.getTarget().getClass().getSimpleName());
                String entityId   = extractId(result);
                saveLog(action, entityName, entityId, null, result);
            }

            // ------------------------------------------------------------------
            // Pointcut: update* trong service
            // ------------------------------------------------------------------
            @AfterReturning(
                    pointcut = "execution(* com.nmcnpm.Homestay.service.*Service.update*(..))",
                    returning = "result"
            )
            public void afterUpdate(JoinPoint jp, Object result) {
                String action     = "UPDATE";
                String entityName = resolveEntityName(jp.getTarget().getClass().getSimpleName());
                String entityId   = extractId(result);
                saveLog(action, entityName, entityId, null, result);
            }

            // ------------------------------------------------------------------
            // Pointcut: delete* trong service — result thường là void
            // Lấy entity id từ argument đầu tiên (String id)
            // ------------------------------------------------------------------
            @AfterReturning(
                    pointcut = "execution(* com.nmcnpm.Homestay.service.*Service.delete*(..))"
            )
            public void afterDelete(JoinPoint jp) {
                String action     = "DELETE";
                String entityName = resolveEntityName(jp.getTarget().getClass().getSimpleName());
                String entityId   = extractIdFromArgs(jp.getArgs());
                saveLog(action, entityName, entityId, null, null);
            }

            // ------------------------------------------------------------------
            // Pointcut: cancel* trong service
            // ------------------------------------------------------------------
            @AfterReturning(
                    pointcut = "execution(* com.nmcnpm.Homestay.service.*Service.cancel*(..))",
                    returning = "result"
            )
            public void afterCancel(JoinPoint jp, Object result) {
                String action     = "CANCEL";
                String entityName = resolveEntityName(jp.getTarget().getClass().getSimpleName());
                String entityId   = extractId(result);
                saveLog(action, entityName, entityId, null, result);
            }

            // ------------------------------------------------------------------
            // Pointcut: updateStatus* / updateTaskState trong service
            // ------------------------------------------------------------------
            @AfterReturning(
                    pointcut = "execution(* com.nmcnpm.Homestay.service.*Service.updateStatus(..))"
                            + " || execution(* com.nmcnpm.Homestay.service.*Service.updateTaskState(..))",
                    returning = "result"
            )
            public void afterStatusChange(JoinPoint jp, Object result) {
                String action     = "STATUS_CHANGE";
                String entityName = resolveEntityName(jp.getTarget().getClass().getSimpleName());
                String entityId   = extractId(result);
                saveLog(action, entityName, entityId, null, result);
            }

            // =========================================================================
            // Helpers
            // =========================================================================

            private void saveLog(String action, String entityName,
                                 String entityId, Object oldData, Object newData) {
                try {
                    // Lấy actor từ SecurityContext
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    String actorEmail   = (auth != null) ? auth.getName() : "system";

                    Account actor = null;
                    AccountRole actorRole = null;
                    if (!"system".equals(actorEmail) && !actorEmail.isBlank()) {
                        actor = accountRepository.findByEmail(actorEmail).orElse(null);
                        if (actor != null) actorRole = actor.getRole();
                    }

                    // Serialize newData -> Map<String, Object>
                    Map<String, Object> newDataMap = toMap(newData);
                    Map<String, Object> oldDataMap = toMap(oldData);

                    AuditLog auditLog = AuditLog.builder()
                            .actorAccount(actor)
                            .actorRole(actorRole)
                            .action(action)
                            .entityName(entityName)
                            .entityId(entityId != null ? entityId : "unknown")
                            .oldData(oldDataMap)
                            .newData(newDataMap)
                            .build();

                    auditLogRepository.save(auditLog);

                } catch (Exception ex) {
                    // Không để lỗi audit làm hỏng main flow
                    log.warn("AuditAspect: Failed to save audit log for action={} entity={}: {}",
                            action, entityName, ex.getMessage());
                }
            }

            /**
             * Chuyển ServiceName thành entity name ngắn gọn.
             * VD: "BookingService" -> "bookings", "RoomService" -> "rooms"
             */
            private String resolveEntityName(String serviceClassName) {
                return serviceClassName
                        .replace("Service", "")
                        .toLowerCase() + "s";
            }

            /**
             * Trích entityId từ response DTO bằng cách gọi getId() hoặc đọc field "id".
             */
            private String extractId(Object result) {
                if (result == null) return null;
                try {
                    // Thử gọi getId() reflection
                    var method = result.getClass().getMethod("getId");
                    Object id = method.invoke(result);
                    return id != null ? id.toString() : null;
                } catch (Exception e) {
                    // Fallback: serialize và đọc field "id"
                    try {
                        Map<String, Object> map = toMap(result);
                        if (map != null && map.containsKey("id")) {
                            return String.valueOf(map.get("id"));
                        }
                    } catch (Exception ignored) {}
                }
                return null;
            }

            /**
             * Trích entityId từ danh sách args của method.
             * Convention: argument đầu tiên là id (String hoặc Long).
             */
            private String extractIdFromArgs(Object[] args) {
                if (args == null || args.length == 0) return null;
                Object first = args[0];
                return first != null ? first.toString() : null;
            }

            /**
             * Serialize object thành Map<String, Object> dùng Jackson.
             * Trả về null nếu không serialize được.
             */
            @SuppressWarnings("unchecked")
            private Map<String, Object> toMap(Object obj) {
                if (obj == null) return null;
                try {
                    return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    return null;
                }
            }
        }