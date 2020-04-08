package co.q64.deception.state

import co.q64.deception.Game
import co.q64.deception.state.states.*

enum class GameState(val generator: (Game) -> State) {
    WAITING({ WaitingState }), STARTING({ StartingState(it) }), INTRO({ IntroState(it) }),
    ASSIGNMENT_START({ AssignmentStartState(it) }), ASSIGNMENT_MESSAGE({ AssignmentMessageState(it) }), ASSIGNMENT_DISCUSS({ AssignmentDiscussState(it) }),

    OPERATION_INTRO({ OperationIntroState(it) }),
    OPERATION_START({ OperationStartState(it) }), OPERATION_ACTION({ OperationActionState(it) }), OPERATION_DISCUSS({ OperationDiscussState(it) }),
    ACCUSATION_INTRO({ AccusationIntroState(it) }),
    ACCUSATION_VOTE({ AccusationVoteState(it) }), //RESULTS;
}