package com.softwaremill.codebrag.rest

import org.scalatra.{BadRequest, NotFound}
import com.softwaremill.codebrag.service.diff.DiffWithCommentsService
import org.bson.types.ObjectId
import CommitsEndpoint._
import com.softwaremill.codebrag.common.paging.PagingCriteria
import PagingCriteria.Direction
import com.softwaremill.codebrag.activities.ReviewCommitUseCase
import com.softwaremill.codebrag.common.paging.PagingCriteria
import com.softwaremill.codebrag.activities.finders.AllCommitsFinder
import com.softwaremill.codebrag.activities.finders.toreview.ToReviewCommitsFinder

trait CommitsEndpoint extends JsonServletWithAuthentication {

  def diffService: DiffWithCommentsService

  def reviewableCommitsListFinder: ToReviewCommitsFinder
  def allCommitsFinder: AllCommitsFinder

  def reviewCommitUseCase: ReviewCommitUseCase

  before() {
    haltIfNotAuthenticated
  }

  get("/:sha") {
    val commitSha = params("sha")
    diffService.diffWithCommentsFor(commitSha, userId) match {
      case Right(commitWithComments) => commitWithComments
      case Left(error) => NotFound(error)
    }
  }

  delete("/:sha") {
    reviewCommitUseCase.execute(params("sha"), userId).left.map { err =>
      BadRequest(Map("err" -> err))
    }
  }

  get("/", allCommits) {
    val paging = extractPagingCriteria
    logger.debug(s"Attempting to fetch all commits with possible paging: ${paging}")
    allCommitsFinder.find(userId, extractBranch.getOrElse(EmptyBranchName), paging)
  }

  get("/", commitsToReview) {
    val paging = extractPagingCriteria
    logger.debug(s"Attempting to fetch commits to review with possible paging: ${paging}")
    reviewableCommitsListFinder.find(userId, extractBranch, paging)
  }

  get("/", contextualCommits) {
    val limit = params.getOrElse(LimitParamName, DefaultPageLimit.toString).toInt
    val paging = params.get(SelectedShaParamName) match {
      case Some(commitSha) => PagingCriteria(commitSha, Direction.Radial, limit)
      case None => PagingCriteria.fromEnd[String](limit)
    }
    logger.debug(s"Attempting to fetch commits in context: ${paging}")
    allCommitsFinder.find(userId, extractBranch.getOrElse(EmptyBranchName), paging)
  }

  private def userId = new ObjectId(user.id)

  private def contextualCommits = params.get(ContextParamName).isDefined
  private def commitsToReview = params.get(FilterParamName).isDefined && params(FilterParamName) == ToReviewCommitsFilter
  private def allCommits = params.get(FilterParamName).isDefined && params(FilterParamName) == AllCommitsFilter

  private def extractPagingCriteria = {
    val minSha = params.get(MinShaParamName)
    val maxSha = params.get(MaxShaParamName)
    val limit = params.getOrElse(LimitParamName, CommitsEndpoint.DefaultPageLimit.toString).toInt

    if(maxSha.isDefined) {
      PagingCriteria(maxSha, Direction.Left, limit)
    } else if(minSha.isDefined) {
      PagingCriteria(minSha, Direction.Right, limit)
    } else {
      PagingCriteria.fromBeginning[String](limit)
    }
  }

  private def extractBranch = params.get(BranchParamName)

}

object CommitsEndpoint {

  val DefaultSurroundingsCount = 7
  val DefaultPageLimit = 7

  val ContextParamName = "context"
  val LimitParamName = "limit"
  val BranchParamName = "branch"

  val FilterParamName = "filter"
  val AllCommitsFilter = "all"
  val ToReviewCommitsFilter = "to_review"

  val MinShaParamName = "min_sha"
  val MaxShaParamName = "max_sha"
  val SelectedShaParamName = "selected_sha"

  val EmptyBranchName = ""

}