package com.nbfc.riskgovernance;
class GovernanceException extends RuntimeException {
    final int status;
    GovernanceException(int status,String message) { super(message); this.status=status; }
    static GovernanceException notFound(String name) { return new GovernanceException(404,name+" was not found."); }
    static GovernanceException conflict(String message) { return new GovernanceException(409,message); }
    static GovernanceException forbidden(String message) { return new GovernanceException(403,message); }
}

