package spullara;

import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by sam on 6/18/13.
 */
public class CollectorTest {

    public static final double PASS_THRESHOLD = 70;

    @Test
    public void testCollectors() {

        ArrayList<Person> people = new ArrayList<>();
        ArrayList<Integer> things = new ArrayList<>();
        ArrayList<Employee> employees = new ArrayList<>();
        ArrayList<Student> students = new ArrayList<>();

        // Accumulate names into a List
        List<String> list = people.stream().map(Person::getName).collect(Collectors.toList());

        // Accumulate names into a TreeSet
        Set<String> list2 = people.stream().map(Person::getName).collect(Collectors.toCollection(TreeSet::new));

        // Convert elements to strings and concatenate them, separated by commas
        String joined = things.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        // Find highest-paid employee
        Optional<Employee> highestPaid = employees.stream()
                .collect(Collectors.maxBy(Comparator.comparing(Employee::getSalary)));

        // Group employees by department
        Map<Department, List<Employee>> byDept
                = employees.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment));

        // Find highest-paid employee by department
        Map<Department, Optional<Employee>> highestPaidByDept
                = employees.stream().collect(Collectors.groupingBy(Employee::getDepartment,
                        Collectors.maxBy(Comparator.comparing(Employee::getSalary))));

        // Partition students into passing and failing
        Map<Boolean, List<Student>> passingFailing =
                students.stream()
                        .collect(Collectors.partitioningBy(s -> s.getGrade() >= PASS_THRESHOLD));
    }

    private class Employee {

        public Department getDepartment() {
            return new Department();
        }
        public int getSalary() {
            return 1000;
        }
    }

    private class Department {
    }

    private class Person {
        public String getName() {
            return "name";
        }
    }

    private class Student {
        public int getGrade() { return 100; }
    }
}
