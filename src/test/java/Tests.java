import com.tqhy.client.ClientApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Yiheng
 * @create 1/29/2019
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ClientApplication.class)
public class Tests {

    Logger logger = LoggerFactory.getLogger(Tests.class);

    @Test
    public void test() {
    }
}
