import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
    state: {
        isAccountModalVisible: false,
        currentModel: "deepseek-v3",
        token: localStorage.getItem('token') || null,
        user: JSON.parse(localStorage.getItem('user')) || null,
        isAuthenticated: !!localStorage.getItem('token'),
        availableModels: [
            { id: 1, name: 'Kimi', description: '获得日常帮助', tag: '新' },
            { id: 2, name: 'DeepSeek', description: '具备高级推理能力', tag: '新' },
            { id: 3, name: 'GPT-4o', description: '处理复杂任务效果最好', tag: '新' }
        ]
    },
    mutations: {
        TOGGLE_ACCOUNT_MODAL(state) {
            state.isAccountModalVisible = !state.isAccountModalVisible
        },
        SET_CURRENT_MODEL(state, modelName) {
            state.currentModel = modelName
        },
        SET_USER(state, user) {
            state.user = user;
            state.isAuthenticated = true;
            localStorage.setItem('user', JSON.stringify(user));
        },
        SET_TOKEN(state, token) {
            state.token = token;
            localStorage.setItem('token', token);
        },
        CLEAR_USER(state) {
            state.user = null;
            state.isAuthenticated = false;
            state.token = null;
            localStorage.removeItem('user');
            localStorage.removeItem('token');
        }
    },
    actions: {
        toggleAccountModal({ commit }) {
            commit('TOGGLE_ACCOUNT_MODAL')
        },
        setCurrentModel({ commit }, modelName) {
            commit('SET_CURRENT_MODEL', modelName)
        },
        login({ commit }, { token, user }) {
            commit('SET_TOKEN', token);
            commit('SET_USER', user);
        },
        logout({ commit }) {
            commit('CLEAR_USER');
        }
    },
    getters: {
        isAccountModalVisible: state => state.isAccountModalVisible,
        currentModel: state => state.currentModel,
        userEmail: state => state.user ? state.user.email : '',
        username: state => state.user ? state.user.username : '',
        availableModels: state => state.availableModels,
        isAdmin: state => state.user && state.user.role === 'admin',
        currentUser: state => state.user,
        isLoggedIn: state => state.isAuthenticated,
        token: state => state.token
    }
})