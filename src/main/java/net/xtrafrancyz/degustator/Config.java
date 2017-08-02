package net.xtrafrancyz.degustator;

/**
 * @author xtrafrancyz
 */
public class Config {
    public String token = "MY_BOT_TOKEN";
    public String vimeApiToken = "VIMEWORLD_API_TOKEN";
    public Mysql mysql = new Mysql();
    public Web web = new Web();
    
    public static class Mysql {
        public String url = "jdbc:mysql://127.0.0.1/degustator?useUnicode=true&characterEncoding=utf-8";
        public String user = "root";
        public String pass = "";
        public int poolSize = 1;
    }
    
    public static class Web {
        public String host = "0.0.0.0";
        public int port = 8765;
        public String secret = "secret";
    }
}
