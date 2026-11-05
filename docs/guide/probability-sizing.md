---
navigation:
  title: Probability Sizing
  icon: statpatterns:probability_pattern
  parent: index.md
  position: 20
---

# Probability Sizing

This page explains how the mod decides how many attempts to run so that you reliably reach your target.

## The Binomial Model

Each attempt succeeds with probability $p$. Running $n$ attempts, the number of successes $X$ follows a **binomial distribution**:

$$X \sim \mathrm{Bin}(n, p)$$

For a request of $N$ items, the mod finds the smallest $n$ such that the risk of producing less than $N$ is at most $\alpha$:

$$\Pr(X < N) \leq \alpha$$

## Small Batches

For small targets (up to 30 items) the binomial lower tail is summed exactly:

$$\Pr(X \leq N-1) = \sum_{k=0}^{N-1} \binom{n}{k} p^k (1-p)^{n-k}$$

## Large Batches

For larger targets the mod switches to a **normal approximation**:

$$\mu = np, \qquad \sigma^2 = np(1-p)$$

It finds the smallest $n$ such that the standardized margin reaches the critical value $z_\alpha$ (≈ 1.645 for 95% confidence, ≈ 2.326 for 99%).

## Example

With $p = 0.8$ and a target of 100 items, the naive guess $\lceil 100/0.8 \rceil = 125$ attempts would only reach the target about half of the time. The mod therefore plans more attempts, so that you get at least 100 items with the chosen confidence.

## Chain Crafting

The planned attempts scale the per-attempt inputs — the provider is pushed `inputs × attempts` in one batch. Each level of a chain craft is sized independently, so intermediate outputs are guaranteed with the same confidence.
