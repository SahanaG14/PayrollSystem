import java.util.ArrayList;

public class Company {
    String name;
    String address;
    ArrayList<Employee> employees = new ArrayList<>();

    public Company(String name, String address) {
        this.name = name;
        this.address = address;
    }
}