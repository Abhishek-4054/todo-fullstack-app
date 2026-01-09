import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from './App';
import axios from 'axios';

// Mock axios using the manual mock
jest.mock('axios');

describe('App Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    axios.get.mockResolvedValue({ data: [] });
    axios.post.mockResolvedValue({ 
      data: { id: 1, title: 'Test', description: 'Test', completed: false } 
    });
    axios.put.mockResolvedValue({ data: {} });
    axios.delete.mockResolvedValue({ data: {} });
  });

  test('renders todo application title', async () => {
    render(<App />);
    
    await waitFor(() => {
      const titleElement = screen.getByText(/Todo Application/i);
      expect(titleElement).toBeInTheDocument();
    });
  });

  test('renders add todo form elements', async () => {
    render(<App />);
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Enter todo title/i)).toBeInTheDocument();
      expect(screen.getByText(/Add Todo/i)).toBeInTheDocument();
    });
  });

  test('shows empty state message', async () => {
    render(<App />);
    
    await waitFor(() => {
      expect(screen.getByText(/No todos yet/i)).toBeInTheDocument();
    });
  });
});