package se.matchday.backend.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

class TokenService {
	private final Clock clock;
	private final String secret;
	private final Duration accessTtl;

	TokenService(Clock clock, SecretKey key, Duration accessTtl) {
		this.clock = clock;
		this.secret = secret;
		this.accessTtl = accessTtl;
	}

	String issueAccessToken(UUID subject){
		throw new UnsupportedOperationException("Not implemented yet");
	}

	String issueRefreshToken(UUID subject){
		throw new UnsupportedOperationException("Not implemented yet");
	}

	VerificationResult verifyAccessToken(String token, TokenPurpose expected){
		throw new UnsupportedOperationException("Not implemented yet");
	}

}

enum TokenPurpose {
	ACCESS,
	REFRESH
}


record VerifiedToken(UUID subject, Instant expiresAt){

}


sealed interface VerificationResult {
	record Valid(VerifiedToken token) implements VerificationResult {}
	record Expired() implements VerificationResult {}
	record WrongSignature() implements VerificationResult {}
	record WrongPurpose() implements VerificationResult {}
	record Malformed() implements VerificationResult {}
}
