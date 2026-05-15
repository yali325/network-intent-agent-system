# MAC-TAV Frontend

Phase 7 前端 Demo 可视化界面，基于 Vue 3、Vite、TypeScript、Vue Router、Pinia、Axios、Ant Design Vue 和 Vue-ECharts。

## 启动顺序

1. 启动后端 `mac-tav-web`。

   ```bash
   mvn -pl mac-tav-web -am spring-boot:run
   ```

2. 启动前端。

   ```bash
   cd mac-tav-frontend
   npm install
   npm run dev
   ```

3. 浏览器打开 Vite 输出的本地地址，默认通常是 `http://localhost:5173`。

## API 地址

前端 axios 默认使用 `baseURL=/api`。开发环境下，`vite.config.ts` 已配置代理：

```ts
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

如果后端不在 `http://localhost:8080`，可以通过环境变量覆盖：

```bash
VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

Windows PowerShell：

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080/api"; npm run dev
```

## 可用命令

```bash
npm install
npm run dev
npm run build
```

## 页面

- `/`：提交自然语言网络需求，调用 `POST /api/demo/tasks`。
- `/tasks/:taskId`：查询并展示完整 `NetworkWorkspace`，包含任务概览、意图解析、网络规划/拓扑图、配置生成、执行结果、验证报告和 Agent 执行日志。
