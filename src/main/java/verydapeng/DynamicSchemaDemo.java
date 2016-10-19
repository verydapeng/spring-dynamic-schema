package verydapeng;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;

@SpringBootApplication
public class DynamicSchemaDemo {

    public static void main(String[] args) {
        SpringApplication.run(DynamicSchemaDemo.class, args);
    }

    private volatile boolean useDatasource1 = true;

    @Bean
    DataSource dataSource() {

        DataSource ds1 = createDatasource("db1");
        DataSource ds2 = createDatasource("db2");

        return (DataSource) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{DataSource.class},
                (proxy, method, args) -> method.invoke(useDatasource1 ? ds1 : ds2, args));
    }


    private DataSource createDatasource(String name) {
        DataSource ds = DataSourceBuilder.create().url("jdbc:h2:mem:" + name).build();
        DatabasePopulatorUtils.execute(new ResourceDatabasePopulator(
                new InputStreamResource(this.getClass().getResourceAsStream("/db.sql"))
        ), ds);

        return ds;
    }


    @Bean
    ApplicationRunner applicationRunner(PersonRepo repo) {
        return args -> {

            repo.save(new Person(1, "dapeng"));
            System.out.println("ds1: " + repo.findAll());

            useDatasource1 = false;
            System.out.println("ds2: " + repo.findAll());

            useDatasource1 = true;
            System.out.println("ds1: " + repo.findAll());

        };
    }
}

@Entity
@Data
@NoArgsConstructor
class Person {

    @Id
    int id;
    String name;

    Person(int id, String name) {
        this.id = id;
        this.name = name;
    }
}


interface PersonRepo extends JpaRepository<Person, Integer> {}