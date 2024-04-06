package com.canopas.yourspace.data.utils

object Config {

    const val PRIVACY_POLICY_URL = "https://canopas.github.io/your-space/"

    const val RADIUS_TO_CHECK_USER_STATE = 100.0

    /**
     * The minimum distance to change updates in meters, happens when location is turned off and
     * user is moving.
     */
    const val DISTANCE_TO_CHECK_SUDDEN_LOCATION_CHANGE = 2000.0 // 2 km

    const val FIRESTORE_COLLECTION_USERS = "users"
    const val FIRESTORE_COLLECTION_USER_SESSIONS = "user_sessions"
    const val FIRESTORE_COLLECTION_SPACES = "spaces"
    const val FIRESTORE_COLLECTION_SPACE_MEMBERS = "space_members"
    const val FIRESTORE_COLLECTION_SPACE_INVITATION = "space_invitations"
    const val FIRESTORE_COLLECTION_SPACE_THREADS = "space_threads"
    const val FIRESTORE_COLLECTION_THREAD_MESSAGES = "thread_messages"

    const val FIRESTORE_COLLECTION_USER_LOCATIONS = "user_locations"
    const val FIRESTORE_COLLECTION_USER_JOURNEYS = "user_journeys"
}
