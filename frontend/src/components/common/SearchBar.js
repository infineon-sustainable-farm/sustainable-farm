import React, { useState } from 'react';
import './SearchBar.css';

const SearchBar = ({ 
  onSearch, 
  placeholder = 'Search...', 
  initialValue = '',
  showSuggestions = false,
  suggestions = [],
  onSuggestionClick 
}) => {
  const [searchTerm, setSearchTerm] = useState(initialValue);
  const [showSuggestionsList, setShowSuggestionsList] = useState(false);

  const handleChange = (e) => {
    const value = e.target.value;
    setSearchTerm(value);
    onSearch(value);
    setShowSuggestionsList(showSuggestions && value.length > 0);
  };

  const handleSuggestionClick = (suggestion) => {
    setSearchTerm(suggestion);
    onSearch(suggestion);
    setShowSuggestionsList(false);
    if (onSuggestionClick) {
      onSuggestionClick(suggestion);
    }
  };

  const handleClear = () => {
    setSearchTerm('');
    onSearch('');
    setShowSuggestionsList(false);
  };

  return (
    <div className="search-bar-container">
      <div className="search-input-wrapper">
        <svg 
          className="search-icon" 
          width="20" 
          height="20" 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          strokeWidth="2"
        >
          <circle cx="11" cy="11" r="8" />
          <path d="M21 21l-4.35-4.35" />
        </svg>
        <input
          type="text"
          value={searchTerm}
          onChange={handleChange}
          placeholder={placeholder}
          className="search-input"
          onFocus={() => setShowSuggestionsList(showSuggestions && searchTerm.length > 0)}
          onBlur={() => setTimeout(() => setShowSuggestionsList(false), 200)}
        />
        {searchTerm && (
          <button 
            onClick={handleClear}
            className="clear-button"
            aria-label="Clear search"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        )}
      </div>
      
      {showSuggestionsList && suggestions.length > 0 && (
        <div className="search-suggestions">
          {suggestions.map((suggestion, index) => (
            <div
              key={index}
              className="suggestion-item"
              onClick={() => handleSuggestionClick(suggestion)}
            >
              {suggestion}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SearchBar;