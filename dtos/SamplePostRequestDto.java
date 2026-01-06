/**
 * POST 요청용 validation 필수 체크 포함
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SamplePostRequestDto {

    /**
     * 샘플 식별 코드
     */
    @NotBlank(message = "식별 코드는 필수 값입니다.")
    private String sampleKey;

    /**
     * 참조 번호
     */
    @NotBlank(message = "참조 번호는 필수 값입니다.")
    private String referenceNo;

    /**
     * 단계 번호
     */
    @NotBlank(message = "단계 번호는 필수 값입니다.")
    private String stepNo;

    /**
     * 유형 구분 (1, 2)
     */
    @NotBlank(message = "유형 값은 필수 값입니다.")
    @Pattern(regexp = "^[12]$", message = "유형 값은 1 또는 2만 허용됩니다.")
    private String typeFlag;

    /**
     * 처리 일자 (YYYYMMDD)
     */
    @NotBlank(message = "처리 일자는 필수 값입니다.")
    private String actionDate;

    /**
     * 기본 코드
     */
    @NotBlank(message = "기본 코드는 필수 값입니다.")
    private String baseCode;

    /**
     * 위치 코드
     */
    @NotBlank(message = "위치 코드는 필수 값입니다.")
    private String locationCode;

    /**
     * 그룹 코드
     */
    @NotBlank(message = "그룹 코드는 필수 값입니다.")
    private String groupCode;

    /**
     * 조직 코드
     */
    @NotBlank(message = "조직 코드는 필수 값입니다.")
    private String orgCode;

    /**
     * 사용자 코드
     */
    @NotBlank(message = "사용자 코드는 필수 값입니다.")
    private String userCode;

    /**
     * 대상 코드
     */
    @NotBlank(message = "대상 코드는 필수 값입니다.")
    private String targetCode;

    /**
     * 처리 수량
     */
    @NotBlank(message = "처리 수량은 필수 값입니다.")
    private String processedQty;

    /**
     * 성공 수량
     */
    @NotBlank(message = "성공 수량은 필수 값입니다.")
    private String successQty;

    /**
     * 실패 수량
     */
    @NotBlank(message = "실패 수량은 필수 값입니다.")
    private String failureQty;

    /**
     * 시작 단계 여부 (00, 01)
     */
    @NotBlank(message = "시작 단계 여부는 필수 값입니다.")
    @Pattern(regexp = "^(00|01)$", message = "00 또는 01만 허용됩니다.")
    private String startFlag;

    /**
     * 종료 단계 여부 (00, 01)
     */
    @NotBlank(message = "종료 단계 여부는 필수 값입니다.")
    @Pattern(regexp = "^(00|01)$", message = "00 또는 01만 허용됩니다.")
    private String endFlag;

    /**
     * 이동 단계 번호
     */
    @NotBlank(message = "이동 단계 번호는 필수 값입니다.")
    private String nextStepNo;

    /**
     * 이동 대상 코드
     */
    @NotBlank(message = "이동 대상 코드는 필수 값입니다.")
    private String nextTargetCode;

    /**
     * 이동 위치 코드
     */
    @NotBlank(message = "이동 위치 코드는 필수 값입니다.")
    private String nextLocationCode;

    /**
     * 검증 여부 (0, 1)
     */
    @NotBlank(message = "검증 여부는 필수 값입니다.")
    @Pattern(regexp = "^[01]$", message = "0 또는 1만 허용됩니다.")
    private String checkFlag;

    private SamplePostRequestDto(Builder b) {
        this.sampleKey = b.sampleKey;
        this.referenceNo = b.referenceNo;
        this.stepNo = b.stepNo;
        this.typeFlag = b.typeFlag;
        this.actionDate = b.actionDate;
        this.baseCode = b.baseCode;
        this.locationCode = b.locationCode;
        this.groupCode = b.groupCode;
        this.orgCode = b.orgCode;
        this.userCode = b.userCode;
        this.targetCode = b.targetCode;
        this.processedQty = b.processedQty;
        this.successQty = b.successQty;
        this.failureQty = b.failureQty;
        this.startFlag = b.startFlag;
        this.endFlag = b.endFlag;
        this.nextStepNo = b.nextStepNo;
        this.nextTargetCode = b.nextTargetCode;
        this.nextLocationCode = b.nextLocationCode;
        this.checkFlag = b.checkFlag;
    }

    /**
     * Module 레벨에서만 Builder + Validation 호출
     */
    public static Builder builder(Validator validator) {
        return new Builder(validator);
    }

    public static final class Builder {
        private final Validator validator;

        private String sampleKey;
        private String referenceNo;
        private String stepNo;
        private String typeFlag;
        private String actionDate;
        private String baseCode;
        private String locationCode;
        private String groupCode;
        private String orgCode;
        private String userCode;
        private String targetCode;
        private String processedQty;
        private String successQty;
        private String failureQty;
        private String startFlag;
        private String endFlag;
        private String nextStepNo;
        private String nextTargetCode;
        private String nextLocationCode;
        private String checkFlag;

        private Builder(Validator validator) {
            this.validator = validator;
        }

        public Builder sampleKey(String v) { this.sampleKey = v; return this; }
        public Builder referenceNo(String v) { this.referenceNo = v; return this; }
        public Builder stepNo(String v) { this.stepNo = v; return this; }
        public Builder typeFlag(String v) { this.typeFlag = v; return this; }
        public Builder actionDate(String v) { this.actionDate = v; return this; }
        public Builder baseCode(String v) { this.baseCode = v; return this; }
        public Builder locationCode(String v) { this.locationCode = v; return this; }
        public Builder groupCode(String v) { this.groupCode = v; return this; }
        public Builder orgCode(String v) { this.orgCode = v; return this; }
        public Builder userCode(String v) { this.userCode = v; return this; }
        public Builder targetCode(String v) { this.targetCode = v; return this; }
        public Builder processedQty(String v) { this.processedQty = v; return this; }
        public Builder successQty(String v) { this.successQty = v; return this; }
        public Builder failureQty(String v) { this.failureQty = v; return this; }
        public Builder startFlag(String v) { this.startFlag = v; return this; }
        public Builder endFlag(String v) { this.endFlag = v; return this; }
        public Builder nextStepNo(String v) { this.nextStepNo = v; return this; }
        public Builder nextTargetCode(String v) { this.nextTargetCode = v; return this; }
        public Builder nextLocationCode(String v) { this.nextLocationCode = v; return this; }
        public Builder checkFlag(String v) { this.checkFlag = v; return this; }

        public SamplePostRequestDto build() {
            SamplePostRequestDto dto = new SamplePostRequestDto(this);
            Set<ConstraintViolation<SamplePostRequestDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            return dto;
        }
    }
}