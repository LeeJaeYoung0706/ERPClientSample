/**
 * GET 요청 Request DTO 함수 예제 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleGetRequestDto implements CacheKeyProvider {

    /**
     * 샘플 식별 코드
     */
    @NotBlank(message = "식별 코드는 필수 값입니다.")
    private String sampleKey;

    private String sampleType;

    private Integer sampleInteger1;

    private Integer sampleInteger2;
    private void setSampleType(String sampleType) {
        final List<String> typeList = List.of("sampleType1", "sampleType2");
        if (!typeList.contains(sampleType))
            throw new IllegalArgumentException("올바르지 않은 Type 요청입니다.");
        this.sampleType = sampleType;
    }

    // 캐싱 예제라 실무에서는 처리했습니다.
    @Override
    public String cacheKey() {
        return String.join("|",
                String.valueOf(sampleType),
                String.valueOf(sampleInteger1)
        );
    }

    public void setSampleKey(String sampleKey) {
        this.sampleKey = sampleKey;
    }

    public void setSampleInteger1(Integer sampleInteger1) {
        this.sampleInteger1 = sampleInteger1;
    }

    public void setSampleInteger2(Integer sampleInteger2) {
        this.sampleInteger2 = sampleInteger2;
    }

    public static SampleGetRequestDto createSampleType1(Integer sampleInteger1, Integer sampleInteger2, String key) {
        SampleGetRequestDto sampleGetRequestDto = new SampleGetRequestDto();
        sampleGetRequestDto.setSampleType("sampleType1");
        sampleGetRequestDto.setSampleInteger1(sampleInteger1);
        sampleGetRequestDto.setSampleInteger2(sampleInteger2);
        sampleGetRequestDto.setSampleKey(key);
        return sampleGetRequestDto;
    }

    public static SampleGetRequestDto createSampleType2(String key) {
        SampleGetRequestDto sampleGetRequestDto = new SampleGetRequestDto();
        sampleGetRequestDto.setSampleType("sampleType2");
        sampleGetRequestDto.setSampleKey(key);
        return sampleGetRequestDto;
    }
}
