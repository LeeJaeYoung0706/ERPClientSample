@Component
@RequiredArgsConstructor
public class SampleClientModule {


    private final Validator validator;

    /**
     * Error 메세지 생성 함수
     *
     * @param reason
     * @param path
     * @return
     */
    protected String stringFormatErrorReason(String reason, String path) {
        return String.format("ERP 동기화 실패\n사유 : %s\n요청 Path : %s", reason, path);
    }

    /**
     * service 호출 시 검증 처리 로직
     *
     * @param dto
     */
    public void validate(SamplePostRequestDto dto) {
        Set<ConstraintViolation<SamplePostRequestDto>> violations =
                validator.validate(dto);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    // ========= Param Parsing / Validation ========= //

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,                 // yyyy-MM-dd
            DateTimeFormatter.BASIC_ISO_DATE,                 // yyyyMMdd
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    );

    /**
     * Map에서 LocalDate 필수 값 파싱
     * 허용 포맷: yyyy-MM-dd, yyyyMMdd, yyyy-MM-dd HH:mm:ss, yyyyMMddHHmmss
     */
    public LocalDate requireLocalDate(Map<String, Object> param, String key) {
        Object v = param.get(key);

        if (v == null) {
            throw new CustomResponseException(key + " 값이 존재하지 않습니다.", 400);
        }

        if (v instanceof LocalDate) {
            return (LocalDate) v;
        }

        if (v instanceof LocalDateTime) {
            return ((LocalDateTime) v).toLocalDate();
        }

        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        if (v instanceof CharSequence) {
            String s = v.toString().trim();
            if (s.isEmpty()) {
                throw new CustomResponseException(key + " 값이 비어있습니다.", 400);
            }

            // 문자열이 datetime일 수도 있으니 포맷 순회
            for (DateTimeFormatter f : DATE_FORMATS) {
                try {
                    // yyyy-MM-dd HH:mm:ss / yyyyMMddHHmmss 같은 경우 LocalDateTime 우선 시도
                    if (looksLikeDateTime(s, f)) {
                        try {
                            return LocalDateTime.parse(s, f).toLocalDate();
                        } catch (DateTimeParseException ignore) {
                            // 다음 포맷 시도
                        }
                    }
                    return LocalDate.parse(s, f);
                } catch (DateTimeParseException ignore) {
                    // 다음 포맷 시도
                }
            }

            throw new CustomResponseException(
                    key + " 형식이 유효하지 않습니다. 허용: yyyy-MM-dd, yyyyMMdd, yyyy-MM-dd HH:mm:ss, yyyyMMddHHmmss",
                    400
            );
        }

        throw new CustomResponseException(
                key + " 타입이 유효하지 않습니다. (현재: " + v.getClass().getName() + ")",
                400
        );
    }

    /**
     * Map에서 String 필수 값 파싱 (trim 적용)
     */
    public String requireString(Map<String, Object> param, String key) {
        Object v = param.get(key);

        if (v == null) {
            throw new CustomResponseException(key + " 값이 존재하지 않습니다.", 400);
        }

        if (!(v instanceof CharSequence)) {
            throw new CustomResponseException(key + " 타입이 유효하지 않습니다. (문자열 필요)", 400);
        }

        String s = v.toString().trim();
        if (s.isEmpty()) {
            throw new CustomResponseException(key + " 값이 비어있습니다.", 400);
        }
        return s;
    }

    // 휴리스틱
    private boolean looksLikeDateTime(String s, DateTimeFormatter f) {
        return s.length() > 10 || f.toString().contains("HourOfDay") || f.toString().contains("SecondOfMinute");
    }
}
