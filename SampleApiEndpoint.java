/**
 * 외부 시스템 연동용 API Endpoint 샘플 Enum
 * - 실제 URL 및 도메인 의미는 제거
 * - Endpoint 관리 및 오류 메시지 표준화 구조 예제
 */
public enum SampleApiEndpoint {

    SAMPLE_QUERY_A("/sample/api/v1/queryA", "샘플 데이터 조회"),
    SAMPLE_QUERY_B("/sample/api/v1/queryB", "샘플 상세 정보 조회"),
    SAMPLE_COMMAND_CREATE("/sample/api/v1/create", "샘플 데이터 등록"),
    SAMPLE_QUERY_C("/sample/api/v1/queryC", "샘플 경로 정보 조회"),
    SAMPLE_COMMAND_DELETE("/sample/api/v1/delete", "샘플 데이터 삭제"),
    SAMPLE_QUERY_ALL("/sample/api/v1/queryAll", "샘플 전체 목록 조회"),
    SAMPLE_QUERY_D("/sample/api/v1/queryD", "샘플 연관 정보 조회");

    private final String path;
    private final String description;

    SampleApiEndpoint(String path, String description) {
        this.path = path;
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }
}
