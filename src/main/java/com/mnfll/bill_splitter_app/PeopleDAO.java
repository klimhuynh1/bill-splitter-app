package com.mnfll.bill_splitter_app;

public interface PeopleDAO {
    int getPersonIdByName(String personName);
    void deleteOrphanUsers();
}
