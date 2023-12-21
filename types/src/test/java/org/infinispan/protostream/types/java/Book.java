package org.infinispan.protostream.types.java;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Objects;

public class Book implements Comparable<Book> {

    @ProtoField(number = 1)
    String title;

    @ProtoField(number = 2)
    String description;

    @ProtoField(number = 3, defaultValue = "2023")
    int publicationYear;

    @ProtoFactory
    public Book(String title, String description, int publicationYear) {
        this.title = title;
        this.description = description;
        this.publicationYear = publicationYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return publicationYear == book.publicationYear && Objects.equals(title, book.title) && Objects.equals(description, book.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, publicationYear);
    }

    @Override
    public String toString() {
        return "Book{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", publicationYear=" + publicationYear +
                '}';
    }

    @Override
    public int compareTo(Book o) {
        int cmp = Integer.compare(this.publicationYear, o.publicationYear);
        if (cmp != 0) {
            return cmp;
        }
        cmp = title.compareTo(o.title);
        if (cmp != 0) {
            return cmp;
        }
        return description.compareTo(o.description);
    }
}
