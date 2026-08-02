import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

client.interceptors.response.use(
  (res) => res,
  (err) => {
    const isLoginRequest = err.config?.url === '/auth/login'
    if (err.response?.status === 401 && !isLoginRequest) {
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default client
