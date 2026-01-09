package com.example.todoapp;

import com.example.todoapp.controller.TodoController;
import com.example.todoapp.model.Todo;
import com.example.todoapp.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class TodoControllerTest {

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private TodoController todoController;

    private Todo testTodo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testTodo = new Todo();
        testTodo.setId(1L);
        testTodo.setTitle("Test Todo");
        testTodo.setDescription("Test Description");
        testTodo.setCompleted(false);
    }

    @Test
    void testGetAllTodos() {
        // Arrange
        List<Todo> todos = Arrays.asList(testTodo);
        when(todoRepository.findAll()).thenReturn(todos);

        // Act
        List<Todo> result = todoController.getAllTodos();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Todo", result.get(0).getTitle());
        verify(todoRepository, times(1)).findAll();
    }

    @Test
    void testCreateTodo() {
        // Arrange
        when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

        // Act
        Todo result = todoController.createTodo(testTodo);

        // Assert
        assertNotNull(result);
        assertEquals("Test Todo", result.getTitle());
        verify(todoRepository, times(1)).save(testTodo);
    }

    @Test
    void testUpdateTodo() {
        // Arrange
        Todo updatedTodo = new Todo();
        updatedTodo.setTitle("Updated Title");
        updatedTodo.setDescription("Updated Description");
        updatedTodo.setCompleted(true);

        when(todoRepository.findById(1L)).thenReturn(Optional.of(testTodo));
        when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

        // Act
        Todo result = todoController.updateTodo(1L, updatedTodo);

        // Assert
        assertEquals("Updated Title", result.getTitle());
        assertTrue(result.isCompleted());
        verify(todoRepository, times(1)).findById(1L);
        verify(todoRepository, times(1)).save(any(Todo.class));
    }

    @Test
    void testDeleteTodo() {
        // Arrange
        doNothing().when(todoRepository).deleteById(1L);

        // Act
        todoController.deleteTodo(1L);

        // Assert
        verify(todoRepository, times(1)).deleteById(1L);
    }

    @Test
    void testUpdateTodoNotFound() {
        // Arrange
        when(todoRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            todoController.updateTodo(999L, testTodo);
        });
    }
}