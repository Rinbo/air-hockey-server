import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class test_jackson {
    public static void main(String[] args) {
        SimpleModule mod = new SimpleModule();
        ObjectMapper mapper = JsonMapper.builder().addModule(mod).build();
        System.out.println("Success");
    }
}
