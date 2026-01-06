package dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GET 요청 Request DTO 함수 예제 2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleGetRequestSecondDto {

    /**
     * 샘플 식별 코드
     */
    @NotBlank(message = "식별 코드는 필수 값입니다.")
    private String sampleKey;

    private String sampleKey2;

    private final String sampleSt = "1";

    private Integer dateFrom;

    private Integer dateTo;

    private String conDate;

    public void setSampleKey(String sampleKey) {
        this.sampleKey = sampleKey;
    }

    public void setDateFrom(Integer dateFrom) {
        this.dateFrom = dateFrom;
    }

    public void setDateTo(Integer dateTo) {
        this.dateTo = dateTo;
    }

    public void setSampleKey2(String sampleKey2) {
        this.sampleKey2 = sampleKey2;
    }

    public static SampleGetRequestSecondDto createSampleGetRequestSecondDto(SampleGetRequestSecondValidationDto dto) {
        SampleGetRequestSecondDto sampleGetRequestSecondDto = new SampleGetRequestSecondDto();

        LocalDate base = dto.getDate();
        LocalDate from = dto.getDateFrom();
        LocalDate to = dto.getDateTo();
        LocalDate conDate = dto.getConDate();
        String sampleKey2 = dto.getSampleKey2();
        String sampleKey = dto.getSampleKey();

        if (base != null) {
            int yyyymmdd = Integer.parseInt(base.format(DateTimeFormatter.BASIC_ISO_DATE));
            sampleGetRequestSecondDto.setDateFrom(yyyymmdd);
            sampleGetRequestSecondDto.setDateTo(yyyymmdd);
        } else {
            if (from == null || to == null) {
                throw new IllegalArgumentException("올바르지 않은 조회 요청입니다.");
            }
            sampleGetRequestSecondDto.setDateFrom(Integer.parseInt(from.format(DateTimeFormatter.BASIC_ISO_DATE)));
            sampleGetRequestSecondDto.setDateTo(Integer.parseInt(to.format(DateTimeFormatter.BASIC_ISO_DATE)));
        }

        if (conDate != null) {
            LocalDateTime dt = conDate.atTime(0, 0, 1);
            sampleGetRequestSecondDto.setConDate(
                    dt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            );
        }

        if (sampleKey2 != null) {
            sampleGetRequestSecondDto.setSampleKey2(sampleKey2);
        }

        if (sampleGetRequestSecondDto.getSampleKey() == null) {
            if (sampleKey == null)
                throw new IllegalArgumentException("올바르지 않은 조회 요청입니다. sample Key 미입력");
            else
                sampleGetRequestSecondDto.setSampleKey(sampleKey);
        }

        return sampleGetRequestSecondDto;
    }

    public void setConDate(String sampleKey2) {
        if (sampleKey2 == null) {
            this.sampleKey2 = null;
            return;
        }

        if (!sampleKey2.matches("^\\d{14}$")) {
            throw new IllegalArgumentException(
                    "조회 기준일은 yyyyMMddHHmmss 형식이어야 합니다."
            );
        }

        this.sampleKey2 = sampleKey2;
    }
}
