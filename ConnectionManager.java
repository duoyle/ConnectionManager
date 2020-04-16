package pers.xxm.database;

import org.apache.commons.dbcp2.BasicDataSource;
import pers.xxm.resource.ResourceManager;
import pers.xxm.util.AesUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by XuXuemin on 20/2/29
 */
public class ConnectionManager {
    /*
    1. DataSource是DriverManager使用的替代者，建议使用。
    2. CommonDataSource有三个子接口：DataSource（简单功能），ConnectionPoolDataSource（连接池）和XADataSource（事务）
       但是后来没有按照规规划的路子走，很多厂商实现了DataSource一个接口，实现了连接池和事务功能。
    3. org.apache.commons.dbcp.BasicDataSource（C3P0的ComboPooledDataSource和DruidDataSource)，新版在dbcp2中
    4. 借助DataSource获取PooledConnection（包含事件方法）
     */
    private static ThreadLocal<Connection> remoteConnection = new ThreadLocal<>();
    private static DataSource dataSource = new BasicDataSource();

    static {

        // 配置文件：resources/database.properties
        ResourceManager resourceManager = ResourceManager.load("database");
        String driver = resourceManager.getString("jdbc.driver");
        String url = resourceManager.getString("jdbc.url");
        String user = resourceManager.getString("jdbc.user");
        String password = AesUtil.decrypt(resourceManager.getString("jdbc.password"));

        // 下面取代了Class.forName()（或DbUtils.loadDriver）和DriverManager.getConnection()。
        /*
        String url = "jdbc:mysql://localhost:3306/employee?characterEncoding=utf8";
        String userName = "xxm";
        String password = "xuxuemin";
        String driverName = "com.mysql.jdbc.Driver";
        */
        BasicDataSource source = (BasicDataSource) dataSource;
        source.setDriverClassName(driver);
        source.setUrl(url);
        source.setUsername(user);
        source.setPassword(password);
    }

    /**
     * 取得连接对象
     * @return 连接对象
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = remoteConnection.get();
        if (conn == null) {
            conn = dataSource.getConnection();
            remoteConnection.set(conn);
        }
        return conn;
    }

    /**
     * 关闭连接
     */
    public static void close() throws SQLException {
        getConnection().close();
        remoteConnection.remove(); // 移除掉，或者set(null)
    }

    /**
     * 提交事务
     */
    public static void commit() throws SQLException {
        getConnection().commit();
    }

    /**
     * 回滚事务
     */
    public static void rollback() throws SQLException {
        getConnection().rollback();
    }

    /**
     * 设为自动调焦
     * @param flag true是自动提交（默认）；false是事务（手动）提交
     */
    public static void setTransaction(boolean flag) throws SQLException {
        getConnection().setAutoCommit(flag);
    }

    /**
     * 查看当前是否是事务提交
     * @return true是事务（手动）提交；false是自动提交
     */
    public static boolean getTransaction() throws SQLException {
        return !getConnection().getAutoCommit();
    }
}
