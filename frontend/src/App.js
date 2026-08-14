import React from 'react';
import './App.css';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Sustainable Farm - Product Transformation System</h1>
        <p>BIT × Infineon Excellence Program</p>
        <p>Implementation Phase - Week 5</p>
      </header>
      <main className="App-main">
        <div className="dashboard">
          <h2>System Status</h2>
          <div className="status-card">
            <h3>Backend Connection</h3>
            <p className="status-pending">Configuring...</p>
          </div>
          <div className="status-card">
            <h3>Database Connection</h3>
            <p className="status-pending">Configuring...</p>
          </div>
          <div className="status-card">
            <h3>Data Model</h3>
            <p className="status-success">Validated (10.0/10)</p>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;