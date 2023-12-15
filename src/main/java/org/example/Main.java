package org.example;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static final int THREAD_COUNT = 8;
    static SessionFactory configuration = new Configuration()
            .addAnnotatedClass(Items.class)
            .configure("hibernate.cfg.xml").buildSessionFactory();

    public static void main(String[] args) {
        long time = System.currentTimeMillis();

        Session session = configuration.openSession();
        try {
            for (int i = 0; i < 40; i++) {
                session.beginTransaction();
                Items item = new Items();
                session.persist(item);
                session.getTransaction().commit();
            }
            threadTest();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            configuration.close();
            session.close();
        }

        Date date = new Date(System.currentTimeMillis() - time);

        DateFormat formatter = new SimpleDateFormat("mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateFormatted = formatter.format(date);

        System.out.println("Затраченное время " + dateFormatted);
        System.out.println("Количество OptimisticLockException: " + cntOptimisticLock);
        System.out.println("Количество HibernateException: " + cntHibernateException);
        System.out.println("Количество InterruptedException: " + cntInterruptedException);
        System.out.println("Количество Exception: " + cntException);
        System.out.println();
    }

    static int cntOptimisticLock = 0;
    static int cntHibernateException = 0;
    static int cntInterruptedException = 0;
    static int cntException = 0;

    public static void threadTest() {
        CountDownLatch countDownLatch = new CountDownLatch(THREAD_COUNT);
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int ii = i + 1;
            threads[i] = new Thread(() -> {

                for (int k = 0; k < 20000; k++) {
                    boolean upd = false;
                    while (!upd) {
                        Session session = configuration.getCurrentSession();
                        Long rndRow = (long) ((Math.random() * 40) + 1);
                        try {
                            session.beginTransaction();

                            // Use HQL with "for update" to obtain a pessimistic lock
                            String hqlQuery = "SELECT items FROM Items items WHERE items.id = :id";
                            Items items = session.createQuery(hqlQuery, Items.class)
                                    .setParameter("id", rndRow)
                                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                                    .uniqueResult();

                            // Increment the value
                            items.setVal(items.getVal() + 1);

                            Thread.sleep(5);
                            session.getTransaction().commit();
                            upd = true;
                        } catch (OptimisticLockException e) {
                            cntOptimisticLock++;
                            session.getTransaction().rollback();
                        } catch (HibernateException ee) {
                            cntHibernateException++;
                            ee.printStackTrace();
                        } catch (InterruptedException ee) {
                            cntInterruptedException++;
                            ee.printStackTrace();
                        } catch (Exception ee) {
                            cntException++;
                            ee.printStackTrace();
                        } finally {
                            session.close();
                        }
                    }
                }
                countDownLatch.countDown();
            });
            threads[i].start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Session session = configuration.openSession();
            session.beginTransaction();
            // HQL запрос для вычисления суммы
            String hqlQuery = "SELECT SUM(val) FROM Items";
            Query<Long> query = session.createQuery(hqlQuery, Long.class);
            Long sum = query.uniqueResult();
            System.out.println("сумма элементов: " + sum);
            session.getTransaction().commit();
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            configuration.close();
        }
    }
}