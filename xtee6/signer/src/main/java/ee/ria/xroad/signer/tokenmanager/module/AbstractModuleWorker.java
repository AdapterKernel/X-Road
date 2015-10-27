package ee.ria.xroad.signer.tokenmanager.module;

import java.util.List;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.Duration;

import ee.ria.xroad.signer.protocol.dto.TokenInfo;
import ee.ria.xroad.signer.tokenmanager.TokenManager;
import ee.ria.xroad.signer.tokenmanager.token.TokenType;
import ee.ria.xroad.signer.util.AbstractUpdateableActor;
import ee.ria.xroad.signer.util.Update;

/**
 * Module worker base class.
 */
@Slf4j
public abstract class AbstractModuleWorker extends AbstractUpdateableActor {

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(-1, Duration.Inf(),
            t -> SupervisorStrategy.resume()
        );
    }

    @Override
    public void preStart() throws Exception {
        try {
            initializeModule();
        } catch (Exception e) {
            log.error("Failed to initialize module", e);
            getContext().stop(getSelf());
        }
    }

    @Override
    public void postStop() throws Exception {
        try {
            deinitializeModule();
        } catch (Exception e) {
            log.error("Failed to deinitialize module", e);
        }
    }

    @Override
    protected void onUpdate() throws Exception {
        try {
            List<TokenType> tokens = listTokens();

            log.trace("Got {} tokens from module '{}'", tokens.size(),
                    getSelf().path().name());

            updateTokens(tokens);
        } catch (Exception e) {
            log.error("Error during update of module "
                    + getSelf().path().name(), e);
            throw e;
        }
    }

    @Override
    protected void onMessage(Object message) throws Exception {
        unhandled(message);
    }

    protected abstract void initializeModule() throws Exception;
    protected abstract void deinitializeModule() throws Exception;

    protected abstract List<TokenType> listTokens() throws Exception;

    protected abstract Props props(TokenInfo tokenInfo, TokenType tokenType);

    private void updateTokens(List<TokenType> tokens) {
        // create new tokens
        for (TokenType tokenType : tokens) {
            if (!hasToken(tokenType)) {
                createToken(getTokenInfo(tokenType), tokenType);
            }
        }

        // cleanup lost tokens, update existing tokens
        for (ActorRef token : getContext().getChildren()) {
            if (!hasToken(tokens, token)) {
                destroyToken(token);
            } else {
                token.tell(new Update(), getSelf());
            }
        }
    }

    private boolean hasToken(List<TokenType> tokens, ActorRef token) {
        return tokens.stream()
                .filter(t -> t.getId().equals(token.path().name()))
                .findFirst()
                .isPresent();
    }

    private boolean hasToken(TokenType tokenType) {
        return getToken(tokenType) != null;
    }

    private ActorRef getToken(TokenType tokenType) {
        return getContext().getChild(tokenType.getId());
    }

    private ActorRef createToken(TokenInfo tokenInfo, TokenType tokenType) {
        log.debug("Adding new token '{}#{}'", tokenType.getModuleType(),
                tokenInfo.getId());

        return getContext().watch(
            getContext().actorOf(props(tokenInfo, tokenType), tokenType.getId())
        );
    }

    private void destroyToken(ActorRef token) {
        log.debug("Lost token '{}'", token.path().name());

        getContext().unwatch(token);
        getContext().stop(token);
    }

    private static TokenInfo getTokenInfo(TokenType tokenType) {
        TokenInfo info = TokenManager.getTokenInfo(tokenType.getId());
        if (info != null) {
            TokenManager.setTokenAvailable(tokenType, true);
            return info;
        } else {
            return TokenManager.createToken(tokenType);
        }
    }
}
