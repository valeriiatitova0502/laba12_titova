package org.example;

import javax.persistence.*;

@Entity
@Table(name = "items")
public class Items {

    // Идентификатор
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    // Значение поля
    @Column(name = "val")
    public long val;

    // Версия записи для  блокировки
    @Version
    long version;

    // Конструктор по умолчанию
    public Items() {
        this.val = 0;
    }

    // Геттер для значения
    public long getVal() {
        return val;
    }

    // Геттер для версии
    public long getVersion() {
        return version;
    }

    // Сеттер для значения
    public void setVal(long val) {
        this.val = val;
    }
}