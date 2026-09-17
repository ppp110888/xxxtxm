// ==========================================
// CodeMate 常量和配置
// ==========================================

/** 判题状态 → 展示信息映射 */
export const JUDGE_STATUS_MAP = {
  PENDING:   { label: '排队中',   color: '#909399', icon: 'Loading',      class: 'status-pending' },
  COMPILING: { label: '编译中',   color: '#409EFF', icon: 'Loading',      class: 'status-pending' },
  RUNNING:   { label: '运行中',   color: '#409EFF', icon: 'Loading',      class: 'status-pending' },
  JUDGING:   { label: '判题中',   color: '#409EFF', icon: 'Loading',      class: 'status-pending' },
  AC:        { label: '通过',     color: '#67C23A', icon: 'CircleCheck',  class: 'status-ac' },
  WA:        { label: '答案错误', color: '#F56C6C', icon: 'CircleClose',  class: 'status-wa' },
  TLE:       { label: '运行超时', color: '#E6A23C', icon: 'Clock',        class: 'status-tle' },
  MLE:       { label: '内存超限', color: '#E6A23C', icon: 'Odometer',     class: 'status-mle' },
  RE:        { label: '运行错误', color: '#F56C6C', icon: 'WarningFilled',class: 'status-re' },
  CE:        { label: '编译错误', color: '#8B5CF6', icon: 'EditPen',      class: 'status-ce' },
  SE:        { label: '系统错误', color: '#909399', icon: 'InfoFilled',   class: 'status-pending' },
}

/** 难度映射 */
export const DIFFICULTY_MAP = {
  easy:   { label: '简单', color: '#67C23A' },
  medium: { label: '中等', color: '#E6A23C' },
  hard:   { label: '困难', color: '#F56C6C' },
}

/** 语言选项 */
export const LANGUAGE_OPTIONS = [
  { label: 'Python', value: 'python' },
  { label: 'Java',   value: 'java' },
]

/** 知识节点进度状态 */
export const PROGRESS_STATUS = {
  LOCKED:     { label: '未解锁',   color: '#C0C4CC', icon: 'Lock' },
  IN_PROGRESS:{ label: '学习中',   color: '#409EFF', icon: 'Loading' },
  CLEARED:    { label: '已通关',   color: '#67C23A', icon: 'CircleCheck' },
}
