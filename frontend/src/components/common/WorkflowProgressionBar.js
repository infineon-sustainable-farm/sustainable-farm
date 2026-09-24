import React from 'react';
import './WorkflowProgressionBar.css';

const WorkflowProgressionBar = ({ currentStatus, onAdvance, canAdvance = true }) => {
  // Define workflow stages in order
  const workflowStages = [
    { key: 'CREATED', label: 'Created', icon: 'assignment' },
    { key: 'INTAKE', label: 'Intake', icon: 'inbox' },
    { key: 'WASHING', label: 'Washing', icon: 'cleaning_services' },
    { key: 'DRYING', label: 'Drying', icon: 'wb_sunny' },
    { key: 'PACKAGING', label: 'Packaging', icon: 'inventory_2' },
    { key: 'COMPLETED', label: 'Completed', icon: 'check_circle' },
    { key: 'SHIPPED', label: 'Shipped', icon: 'local_shipping' }
  ];

  // Handle REJECTED status
  if (currentStatus === 'REJECTED') {
    return (
      <div className="workflow-progression-bar rejected">
        <div className="workflow-stage rejected">
          <span className="material-icons workflow-icon">error</span>
          <span className="workflow-label">Rejected</span>
        </div>
      </div>
    );
  }

  // Find current stage index
  const currentIndex = workflowStages.findIndex(stage => stage.key === currentStatus);
  
  // If status not found, show as unknown
  if (currentIndex === -1) {
    return (
      <div className="workflow-progression-bar">
        <div className="workflow-stage unknown">
          <span className="workflow-label">Unknown Status: {currentStatus}</span>
        </div>
      </div>
    );
  }

  // Determine if can advance (can't advance from SHIPPED or REJECTED)
  const canAdvanceStatus = canAdvance && currentStatus !== 'SHIPPED' && currentStatus !== 'REJECTED';

  return (
    <div className="workflow-progression-bar">
      <div className="workflow-stages">
        {workflowStages.map((stage, index) => {
          let stageClass = 'workflow-stage';
          
          if (index < currentIndex) {
            stageClass += ' completed';
          } else if (index === currentIndex) {
            stageClass += ' current';
          } else {
            stageClass += ' pending';
          }

          return (
            <React.Fragment key={stage.key}>
              <div className={stageClass}>
                <span className="material-icons workflow-icon">{stage.icon}</span>
                <span className="workflow-label">{stage.label}</span>
              </div>
              {index < workflowStages.length - 1 && (
                <div className={`workflow-connector ${index < currentIndex ? 'completed' : ''}`}>
                  <div className="connector-line"></div>
                </div>
              )}
            </React.Fragment>
          );
        })}
      </div>
      
      {canAdvanceStatus && onAdvance && (
        <button 
          className="advance-button"
          onClick={onAdvance}
          title={`Advance from ${currentStatus} to next stage`}
        >
          Advance Status →
        </button>
      )}
    </div>
  );
};

export default WorkflowProgressionBar;