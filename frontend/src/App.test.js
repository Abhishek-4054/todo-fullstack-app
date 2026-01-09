import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import App from './App';
import axios from 'axios';

jest.mock('axios');

describe('App Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    axios.get.mockResolvedValue({ data: [] });
    axios.post.mockResolvedValue({ data: { id: 1, title: 'Test', description: 'Test', completed: false } });
    axios.put.mockResolvedValue({ data: {} });
    axios.delete.mockResolvedValue({ data: {} });
  });

  test('renders todo application title', () => {
    render(<App />);
    expect(screen.getByText(/Todo Application/i)).toBeInTheDocument();
  });

  test('renders add todo form', () => {
    render(<App />);
    expect(screen.getByPlaceholderText(/Enter todo title/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Enter description/i)).toBeInTheDocument();
    expect(screen.getByText(/Add Todo/i)).toBeInTheDocument();
  });

  test('adds a new todo', async () => {
    render(<App />);

    fireEvent.change(screen.getByPlaceholderText(/Enter todo title/i), {
      target: { value: 'New Todo' }
    });
    fireEvent.change(screen.getByPlaceholderText(/Enter description/i), {
      target: { value: 'New Description' }
    });
    fireEvent.click(screen.getByText(/Add Todo/i));

    await waitFor(() => {
      expect(axios.post).toHaveBeenCalledWith(
        'http://localhost:8081/api/todos',
        { title: 'New Todo', description: 'New Description', completed: false }
      );
    });
  });

  test('displays no todos message when list is empty', () => {
    render(<App />);
    expect(screen.getByText(/No todos yet/i)).toBeInTheDocument();
  });

  test('displays todos when data is fetched', async () => {
    axios.get.mockResolvedValueOnce({
      data: [{ id: 1, title: 'Test Todo', description: 'Test Description', completed: false }]
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/Test Todo/i)).toBeInTheDocument();
    });
  });
});
