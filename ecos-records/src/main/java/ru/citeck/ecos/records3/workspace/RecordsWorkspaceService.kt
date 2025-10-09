package ru.citeck.ecos.records3.workspace

interface RecordsWorkspaceService {

    fun isWorkspaceWithGlobalEntities(workspace: String?): Boolean

    /**
     * Retrieves a set of workspace identifiers where the specified user is a member.
     * This method loads **all** workspaces, regardless of whether the membership is direct or indirect.
     *
     * @param user the username for which workspaces are being retrieved
     * @return a set of all workspace identifiers where the user is a member
     */
    fun getUserWorkspaces(user: String): Set<String>
}
