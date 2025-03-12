package com.innercicle.aop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomSpringELParserTest {

    @Test
    void testGetDynamicValueWithSimpleExpression() {
        // given
        String[] parameterNames = {"name", "age"};
        Object[] args = {"John", 30};
        String key = "#name"; // SpEL 표현식

        // when
        Object result = CustomSpringELParser.getDynamicValue(parameterNames, args, key);

        // then
        assertThat(result).isEqualTo("John"); // #name은 "John"으로 평가됨
    }

    @Test
    void testGetDynamicValueWithMathExpression() {
        // given
        String[] parameterNames = {"a", "b"};
        Object[] args = {10, 20};
        String key = "#a + #b"; // SpEL 수학 표현식

        // when
        Object result = CustomSpringELParser.getDynamicValue(parameterNames, args, key);

        // then
        assertThat(result).isEqualTo(30); // #a + #b는 10 + 20 = 30으로 평가됨
    }

    @Test
    void testGetDynamicValueWithComplexExpression() {
        // given
        String[] parameterNames = {"user"};
        Object[] args = {new User("Alice", 25)};
        String key = "#user.name"; // SpEL 복합 표현식

        // when
        Object result = CustomSpringELParser.getDynamicValue(parameterNames, args, key);

        // then
        assertThat(result).isEqualTo("Alice"); // #user.name은 "Alice"로 평가됨
    }

    @Test
    void testGetDynamicValueWithNullExpression() {
        // given
        String[] parameterNames = {"name"};
        Object[] args = {null};
        String key = "#name?.toUpperCase()"; // Null-safe SpEL 표현식

        // when
        Object result = CustomSpringELParser.getDynamicValue(parameterNames, args, key);

        // then
        assertThat(result).isNull(); // #name이 null이므로 결과도 null
    }

    static class User {

        private final String name;
        private final int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

    }

}