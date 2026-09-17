package org.eclipse.jakarta.hello;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ItemMapper {

    @Results(id = "itemMap", value = {
            @Result(column = "ID", property = "id"),
            @Result(column = "NAME", property = "name"),
            @Result(column = "DESCRIPTION", property = "description")
    })
    @Select("SELECT ID, NAME, DESCRIPTION FROM ITEMS ORDER BY ID")
    List<Item> findAll();

    @ResultMap("itemMap")
    @Select("SELECT ID, NAME, DESCRIPTION FROM ITEMS WHERE ID = #{id}")
    Item findById(Long id);

    @Insert("INSERT INTO ITEMS (ID, NAME, DESCRIPTION) VALUES (#{id}, #{name}, #{description})")
    int insert(Item item);

    @Update("UPDATE ITEMS SET NAME = #{name}, DESCRIPTION = #{description} WHERE ID = #{id}")
    int update(Item item);

    @Delete("DELETE FROM ITEMS WHERE ID = #{id}")
    int delete(Long id);
}
