package ru.citeck.ecos.records3.workspace

import ru.citeck.ecos.context.lib.auth.data.AuthData
import ru.citeck.ecos.records2.predicate.model.Predicate

interface RecordsWorkspaceService {

    /**
     * Checks whether the given workspace contains globally shared entities
     * (i.e., entities not scoped to a specific workspace).
     *
     * @param workspace the workspace identifier, may be null
     * @return true if the workspace stores global entities, false otherwise
     */
    fun isWorkspaceWithGlobalEntities(workspace: String?): Boolean

    /**
     * Retrieves a set of workspace identifiers where the specified user is a member.
     * This method loads **all** workspaces, regardless of whether the membership is direct or indirect.
     *
     * @param user the username for which workspaces are being retrieved
     * @return a set of all workspace identifiers where the user is a member
     */
    fun getUserWorkspaces(user: String): Set<String>

    /**
     * Returns a set of workspace identifiers for the given user or workspace-system user.
     *
     * If the provided [auth] represents a workspace-system user, the workspaces associated
     * with that system user are returned. Otherwise, workspaces for the regular user are returned.
     *
     * @param auth authentication data determining which user (regular or ws-system)
     *             the lookup should be performed for
     * @return a set of workspaces available to the user, or `null` if no workspaces can be resolved
     */
    fun getUserOrWsSystemUserWorkspaces(auth: AuthData): Set<String>?

    /**
     * Builds a [Predicate] that filters entities based on the workspaces available
     * to the specified [auth].
     *
     * The resulting predicate ensures that only entities belonging to workspaces
     * accessible by the given user are included. If [queriedWorkspaces] is not empty,
     * the filter will also restrict results to those workspaces that both:
     *  - are listed in [queriedWorkspaces], and
     *  - are accessible to the [auth].
     *
     * @param auth authentication whose access rights
     *             should be considered when constructing the predicate
     * @param queriedWorkspaces a list of workspace identifiers that should be included
     *                          in the filtering scope; may be empty to include all
     *                          accessible workspaces
     * @return a [Predicate] suitable for use in query construction that enforces
     *         workspace-level access control. May return Predicates.alwaysFalse() and Predicates.alwaysTrue()
     */
    fun buildAvailableWorkspacesPredicate(auth: AuthData, queriedWorkspaces: List<String>): Predicate

    /**
     * Returns a set of workspaces available for querying for the given [auth].
     *
     * If [queriedWorkspaces] is empty, all accessible workspaces are returned.
     * Otherwise, the returned set is the intersection between accessible workspaces
     * and explicitly requested ones.
     *
     * @param auth authentication data of the user
     * @param queriedWorkspaces explicit workspace filter, may be empty
     * @return a set of workspaces available to query, or `null` if no workspaces are allowed
     */
    fun getAvailableWorkspacesToQuery(auth: AuthData, queriedWorkspaces: List<String>): Set<String>?
}
