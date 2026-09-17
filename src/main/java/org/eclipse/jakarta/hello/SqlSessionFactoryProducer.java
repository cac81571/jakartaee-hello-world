package org.eclipse.jakarta.hello;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class SqlSessionFactoryProducer {

    @Produces
    @ApplicationScoped
    public SqlSessionFactory produce() {
        DataSource dataSource = lookupDataSource();
        Properties txProps = new Properties();
        txProps.setProperty("closeConnection", "false");
        TransactionFactory transactionFactory = new ManagedTransactionFactory();
        transactionFactory.setProperties(txProps);

        Environment environment = new Environment("jta", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(ItemMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private DataSource lookupDataSource() {
        try {
            InitialContext context = new InitialContext();
            try {
                return (DataSource) context.lookup("jdbc/oracle");
            } catch (NamingException e) {
                return (DataSource) context.lookup("java:comp/env/jdbc/oracle");
            }
        } catch (NamingException e) {
            throw new IllegalStateException("DataSource jdbc/oracle was not found", e);
        }
    }
}
