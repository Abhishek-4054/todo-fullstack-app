package com.example.todoapp;

import com.example.todoapp.model.Todo;
import com.example.todoapp.repository.TodoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TodoRepositoryTest {

    @Autowired
    private TodoRepository todoRepository;

    @Test
    void testSaveTodo() {
        // Arrange
        Todo todo = new Todo();
        todo.setTitle("Test Save");
        todo.setDescription("Testing save functionality");
        todo.setCompleted(false);

        // Act
        Todo savedTodo = todoRepository.save(todo);

        // Assert
        assertNotNull(savedTodo.getId());
        assertEquals("Test Save", savedTodo.getTitle());
    }

    @Test
    void testFindAllTodos() {
        // Arrange
        Todo todo1 = new Todo();
        todo1.setTitle("Todo 1");
        todo1.setCompleted(false);
        
        Todo todo2 = new Todo();
        todo2.setTitle("Todo 2");
        todo2.setCompleted(true);

        todoRepository.save(todo1);
        todoRepository.save(todo2);

        // Act
        List<Todo> todos = todoRepository.findAll();

        // Assert
        assertTrue(todos.size() >= 2);
    }

    @Test
    void testFindById() {
        // Arrange
        Todo todo = new Todo();
        todo.setTitle("Find Me");
        todo.setCompleted(false);
        Todo savedTodo = todoRepository.save(todo);

        // Act
        Optional<Todo> foundTodo = todoRepository.findById(savedTodo.getId());

        // Assert
        assertTrue(foundTodo.isPresent());
        assertEquals("Find Me", foundTodo.get().getTitle());
    }

    @Test
    void testDeleteTodo() {
        // Arrange
        Todo todo = new Todo();
        todo.setTitle("Delete Me");
        Todo savedTodo = todoRepository.save(todo);
        Long id = savedTodo.getId();

        // Act
        todoRepository.deleteById(id);
        Optional<Todo> deletedTodo = todoRepository.findById(id);

        // Assert
        assertFalse(deletedTodo.isPresent());
    }
}