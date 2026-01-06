/**
 * DB Handling Sample Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SampleClientTransactionService {

    private final SampleClientTransactionMapper transactionMapper;

    /**
     * 페이징 X 조회 기능
     * @param paramMap
     * @return
     */
    public List<Map<String, Object>> sampleFindFunction(Map<String, Object> paramMap) {
        return transactionMapper.sampleFindFunction(paramMap);
    }

    /**
     * 페이징 O 조회 기능
     * @param paramMap
     * @return
     */
    public List<Map<String, Object>> sampleFindFunctionPaging(Map<String, Object> paramMap) {
        //해당 함수 참고해서 처리하면 페이징 처리 적용됨. perPageNum, page
        // test 코드 100개 기준 검색 /request/url?page=1&perPageNum=100
        TransUtils.settingCriteria(paramMap);
        return transactionMapper.sampleFindFunction(paramMap);
    }

    /**
     * DB UPSERT 처리 함수
     * @param param 인수받을 개발자가 Map으로 사용하기에 Map으로 설정
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sampleSaveFunction(Map<String, Object> param) {
        log.info("param = " , param.toString());
    }
}
