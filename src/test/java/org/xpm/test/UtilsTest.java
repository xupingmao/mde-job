package org.xpm.test;

import org.junit.Assert;
import org.junit.Test;
import org.xpm.taskpool.util.Utils;

import javax.persistence.Column;
import java.util.*;

/**
 * Created by xupingmao on 2017/12/4.
 */
public class UtilsTest {

    static class Person {
        @Column
        protected String name;
        @Column
        protected int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    static class Worker extends Person {
        @Column
        private String job;

        public String getJob() {
            return job;
        }

        public void setJob(String job) {
            this.job = job;
        }
    }

    @Test
    public void toCamel() {
        Assert.assertEquals("userName", Utils.toCamelName("user_name"));
        Assert.assertEquals("userName", Utils.toCamelName("userName"));
    }

    @Test
    public void toUnderscore() {
        Assert.assertEquals("user_name", Utils.toUnderscoreName("user_name"));
        Assert.assertEquals("user_name", Utils.toUnderscoreName("userName"));
    }

    @Test
    public void getColumnNames() {
        Set<String> columnNames = Utils.getColumnNames(Person.class);
        Set<String> expected = new HashSet<>();
        expected.add("name");
        expected.add("age");
        Assert.assertEquals(expected, columnNames);
        System.out.println(Utils.getColumnNames(Worker.class));
    }
}
