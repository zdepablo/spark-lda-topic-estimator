package org.apache.spark.ml.clustering

import org.apache.spark.ml.param._
import org.apache.spark.sql.DataFrame
import org.apache.spark.rdd.RDD

import breeze.linalg.{CSCMatrix, DenseMatrix, DenseVector, SparseVector, diag, sum, svd}

/**
  * Spectral parameter recovery for the number LDA topics.
  *
  * References:
  *
  * 1. Cheng, Dehua, Xinran He, and Yan Liu. "Model Selection for Topic Models
  * via Spectral Decomposition." AISTATS. 2015.
  * 2. Anandkumar, Anima, et al. "A spectral algorithm for latent dirichlet
  allocation." Advances in Neural Information Processing Systems. 2012.
  * 3. Anandkumar, Animashree, et al. "Tensor decompositions for learning
  * latent variable models." Journal of Machine Learning Research 15.1 (2014):
  * 2773-2832.
  */
trait TopicNumberEstimator extends LDA {
  /**
    * Sensitivity parameter "delta" used to estimate the lower bound of number
    * of topics.
    *
    * For the LDA model, with probability at least 1 − \delta, we have
    *
    * | \sigma_i(\hat{M_2}) - \sigma_i(M_2) | \leq \delta_R,
    *
    * where \sigma_i is i-th singular value, M_2 is a (latent) second-order moment,
    * \hat{M_2} is an estimated second-order moment, and
    * \delta_R = \frac{1}{\sqrt{D \delta}} \sqrt{\frac{2}{L^2} + \frac{2}{V^2} + \epsilon}
    * \epsilon represents higher-order terms.
    */
  final val delta = new DoubleParam(this, "delta", "estimation sensitivity")

  def setDelta(value: Double): this.type = set(delta, value)
  def getDelta: Double = $(delta)

  setDefault(delta -> 0.05)

  private def outer(v: SparseVector[Double]): CSCMatrix[Double] = {
    val prod: CSCMatrix[Double] = CSCMatrix.zeros[Double](v.length, v.length)
    for (i <- 0 until v.activeSize; j <- 0 until v.activeSize) {
      prod(v.indexAt(i), v.indexAt(j)) = v.valueAt(i) * v.valueAt(j)
    }
    prod
  }

  def estimateK(dataset: DataFrame) = {
    val alpha0: Double = if (isSet(docConcentration)) {
      getDocConcentration.length match {
        case 1 => getK * getDocConcentration.head
        case _ => getDocConcentration.sum
      }
    } else {
      1.0
    }

    val documents: RDD[SparseVector[Double]] = LDA.getOldDataset(dataset, $(featuresCol))
      .map { case (_, vec) => SparseVector(vec.toArray: _*) }

    documents.cache()

    val D: Double = documents.count.toDouble

    val V: Double = documents.first.length.toDouble

    val L: Double = documents
      .map { vec => sum(vec) }
      .reduce { _ + _ } / D

    val M1: DenseVector[Double] = documents
      .map { vec => vec / sum(vec) }
      .reduce { _ + _ }
      .map { _ / D }
      .toDenseVector

    logInfo("Finished estimating the first moment.")

    val E_x1_x2: DenseMatrix[Double] = documents
      .map { vec => (outer(vec) - diag(vec)) / (sum(vec) * (sum(vec) - 1)) }
      .reduce { _ + _ }
      .map { _ / D }
      .toDenseMatrix

    documents.unpersist()

    val M2: DenseMatrix[Double] = E_x1_x2 - alpha0 / (alpha0 + 1) * (M1 * M1.t)

    logInfo("Finished estimating the second moment.")

    val svd.SVD(_, sigmas, _) = svd(M2)

    logInfo("Computed the second moment spectrum.")

    val deltaR = Math.sqrt(2.0 / (L*L) + 2.0 / (V*V)) / Math.sqrt(D * getDelta)

    val k = sigmas.findAll(_ >= deltaR).length

    logInfo(s"Lower bound estimate for k is $k.")

    if (isSet(docConcentration)) {
      getDocConcentration.length match {
        case 1 => super.setDocConcentration(1.0 / k).setK(k)
      }
    } else {
      super.setK(k)
    }
  }
}
