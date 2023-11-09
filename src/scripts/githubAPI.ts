let jsonData: object | undefined = null;
async function getJson() {
  if (!jsonData) {
    const safeJson = await import('@/repoInfo.json')
      .then(res => {
        console.log(`Found repo info, using the cache.`)
        return res.default as object;
      })
      .catch(err => {
        console.log(`Found no repo info json, using the API.`)
        return {} as object
      })
    jsonData = safeJson
  }
  return jsonData!
}

export async function getRepoInfo(name: string): Promise<RepoInfo> {
  const safeJson = await getJson()
  const repoInfo = safeJson[name.toLowerCase() as keyof typeof safeJson] as RepoData
  if (repoInfo) {
    return repoInfo.info
  }
  return await fetch(`https://api.github.com/repos/${name}`)
    .then(res => res.json()).then(json => ({...(json as object)} as RepoInfo));
}

export async function getRepoCommits(name: string): Promise<Commit[]> {
  const safeJson = await getJson()
  const repoInfo = safeJson[name.toLowerCase() as keyof typeof safeJson] as RepoData
  if (repoInfo) {
    return repoInfo.commits
  }
  return await fetch(`https://api.github.com/repos/${name}/commits`)
    .then(res => res.json()).then(json => json as Commit[]);
}

export interface GitUser {
  name: string
  email: string
}

export interface GitCommit {
  author: GitUser
  committer: GitUser
  message: string
}

export interface Commit {
  sha: string
  commit: GitCommit

  html_url: string
  author: User | undefined
  committer: User | undefined
}

export interface License {
  name: string
}

export interface User {
  login: string
  avatar_url: string
}

export interface RepoInfo {
  name: string
  full_name: string
  description: string

  owner: User
  stargazers_count: number
  license: License

  fork: boolean
  topics: string[] | undefined
  default_branch: string
}

export interface RepoData {
  info: RepoInfo
  commits: Commit[]
}
