/**
 * ERP 호출 담당 Sample Client
 */
@Service
@Slf4j(topic = "ERROR_FILE_LOGGER")
public class SampleClient {


    private final SampleClientProperties properties;
    private final ObjectMapper OT_OM;
    private final ObjectMapper IN_OM;
    private final SampleClientModule module;

    public SampleClient(SampleClientProperties properties, @Qualifier("externalObjectMapper") ObjectMapper OT_OM, ObjectMapper IN_OM, SampleClientModule module) {
        this.properties = properties;
        this.OT_OM = OT_OM;
        this.IN_OM = IN_OM;
        this.module = module;
    }

    /**
     *
     * @param dto 요청 마다 dto가 달라 제네릭 T 타입으로 설정하여 처리,
     * @param endpoint 요청 엔드포인트에 따라, 에러 메세지 변경 처리 및 엔드포인트 요청
     * @return
     * @param <T>
     */
    public <T> Map<String, Object> callErpApi(T dto, SampleApiEndpoint endpoint) {

        // dto -> Map 변환
        final Map<String, Object> parameterMap;
        try {
            parameterMap = IN_OM.convertValue(dto, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalArgumentException e) {
            String reason = "전송 객체 Map 변환 중 실패.";
            log.error("ERP 동기화 실패 사유 : {} 요청 Path : {}", reason, endpoint.getDescription(), e);
            throw new CustomResponseException(module.stringFormatErrorReason(reason, endpoint.getDescription()), 500);
        }

        return call(parameterMap, endpoint);
    }

    /*====  private  ====*/

    /**
     * Map -> Json String 변환
     *
     * @param param    JSON 으로 변환할 Map
     * @param endpoint 요청 URL 에러 위치 용
     * @return String 타입으로 변환된 값
     */
    protected String parseMapToJSONString(Map<String, Object> param, SampleApiEndpoint endpoint) {
        try {
            return OT_OM.writeValueAsString(param);
        } catch (JsonProcessingException e) {
            String reason = "ERP 파라미터 생성중 JSON 파싱 실패";
            log.error("ERP 동기화 실패 사유 : {} 요청 Path : {}", reason, endpoint.getDescription(), e);
            throw new CustomResponseException(module.stringFormatErrorReason(reason, endpoint.getDescription()), 500);
        }
    }

    /**
     * Sample API 요청용
     *
     * @param parameter 파라미터
     * @param endpoint  요청 endPoint
     * @return
     */
    private String SampleClientServiceInvoke(String parameter, SampleApiEndpoint endpoint) {

        log.info("[ERP paramter] : {}", parameter);
        try {
            return SampleClientRequestService.invoke(
                    endpoint.getPath(),
                    parameter,
                    properties.baseUrl(),
                    properties.token(),
                    properties.hashKey(),
                    properties.calleName(),
                    properties.groupSeq()
            );
        } catch (Exception e) {
            String reason = "API Request Error";
            log.error("ERP 동기화 실패 사유 : {} 요청 Path : {}", reason, endpoint.getDescription(), e);
            throw new CustomResponseException(module.stringFormatErrorReason(reason, endpoint.getDescription()), 500);
        }
    }


    /**
     * API 리턴 검증 및 JSON String to Map
     *
     * @param resultString
     * @param endpoint
     */
    private Map<String, Object> validateApiResult(String resultString, SampleApiEndpoint endpoint) {
        // 결과 값
        String validateString = resultString.trim();
        String errorInfo = endpoint.getDescription();
        if (validateString.equals("")) {
            throw new CustomResponseException(module.stringFormatErrorReason("ERP 요청 데이터 미존재.", errorInfo), 502);
        }
        Map<String, Object> resultMap = toMap(resultString, errorInfo);
        // 결과 데이터 없음.
        if (resultMap == null || resultMap.isEmpty())
            throw new CustomResponseException(module.stringFormatErrorReason("ERP 요청 데이터 결과 미존재.", errorInfo), 502);

        // 성공 값이 아닐 때
        if (!"SUCCESS".equals(String.valueOf(resultMap.get("resultMsg"))))
            throw new CustomResponseException(module.stringFormatErrorReason("ERP 요청 결과 실패\n결과메세지 : " + (resultMap.get("resultMsg") == null ? "메세지가 없습니다." : resultMap.get("resultMsg")), errorInfo), 502);

        return resultMap;
    }

    /**
     * JSON -> Map
     *
     * @param json      JSON String -> Map 으로 변환
     * @param errorInfo 요청 URL 정보
     * @return ERP 결과 값 String -> Map 으로 변환
     */
    private Map<String, Object> toMap(String json, String errorInfo) {
        try {
            return OT_OM.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            String reason = "ERP 요청 데이터 JSON 파싱 실패 .";
            log.error("ERP 동기화 실패 사유 : {} 요청 Path : {}", reason, errorInfo, e);
            throw new CustomResponseException(module.stringFormatErrorReason(reason, errorInfo), 502);
        }
    }

    /**
     * Sample API 호출
     *
     * @param parameterMap 호출 시 필요한 파라미터
     * @param endpoint     호출 URL
     * @return 호출 결과
     */
    private Map<String, Object> call(Map<String, Object> parameterMap, SampleApiEndpoint endpoint) {
        String ERP_PARAMETER = parseMapToJSONString(parameterMap, endpoint);
        String resultString = SampleClientServiceInvoke(
                ERP_PARAMETER,
                endpoint
        );

        return validateApiResult(resultString, endpoint);
    }
}
