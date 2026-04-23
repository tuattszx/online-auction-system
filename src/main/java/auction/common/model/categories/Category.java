package auction.common.model.categories;
import auction.common.model.BaseEntity;

public class Category extends BaseEntity {
    private String name;
    private String description;

    public Category() {
        super();
    }

    public Category(int id, String name, String description) {
        super(id);
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                '}';
    }
}