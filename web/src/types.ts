export type ISODate = `${number}-${number}-${number}`
export type ISODateTime = string
export type ISOTime = `${number}:${number}` | string

export interface Store { storeID: number; storeName: string; storeHours: number }
export interface Permission { permissionID: number; permissionName: string }
export interface Employment { employmentID: number; userID: number; storeID: number }
export interface UserPermissionDTO { userID: number; permissionID: number }
export interface User {
  userID: number
  username: string
  password?: never
  first_name: string
  last_name: string
  nickname: string
  role: string
  bgColour: string
  textColour: string
  inactiveDate: ISODate | null
  employments: Employment[]
  permissions: Permission[]
}

export interface AuthSession {
  user: User
  stores: Store[]
  permissions: Permission[]
  firstLogin: boolean
}

export interface EODDataPoint {
  existsInDB: boolean
  date: ISODate
  storeID: number
  cashAmount: number
  eftposAmount: number
  amexAmount: number
  googleSquareAmount: number
  chequeAmount: number
  medschecks: number
  stockOnHandAmount: number
  scriptsOnFile: number
  smsPatients: number
  tillBalance: number
  runningTillBalance: number
  notes: string
}

export interface TillReportDataPoint {
  storeID: number
  assignedDate: ISODate
  periodStartDate: ISODate | null
  periodEndDate: ISODate | null
  key: string
  quantity: number
  amount: number
}

export interface Target {
  date: ISODate
  storeID: number
  targetName: string
  target1Growth: number
  target1Actual: number
  useTarget1Growth: boolean
  target2Growth: number
  target2Actual: number
  useTarget2Growth: boolean
}

export interface AccountPayment {
  contactName: string
  contactID: number
  storeID: number
  invoiceNumber: string
  invDate: ISODate
  dueDate: ISODate
  description: string
  quantity: number
  unitAmount: number
  accountAdjusted: boolean
  accountCode: string
  taxRate: string
}

export interface AccountPaymentContact {
  contactID: number
  contactName: string
  storeID: number
  accountCode: string
  totalValue: number
}

export interface Invoice {
  supplierName: string
  storeID: number
  supplierID: number
  invoiceNo: string
  invoiceDate: ISODate
  dueDate: ISODate
  description: string
  quantity: number
  unitAmount: number
  importedInvoiceAmount: number
  variance: number
  credits: number
  totalAfterCredits: number
  notes: string
  invoiceExists: boolean
  importExists: boolean
  creditExists: boolean
}

export interface Credit {
  creditID: number
  supplierName: string
  supplierID: number
  storeID: number
  creditNo: string
  creditDate: ISODate
  referenceInvoiceNo: string
  creditAmount: number
  notes: string
}

export interface InvoiceSupplier { contactID: number; supplierName: string; storeID: number }

export interface Shift {
  shiftID: number
  storeID: number
  userID: number
  shiftStartTime: ISOTime
  shiftEndTime: ISOTime
  shiftStartDate: ISODate
  shiftEndDate: ISODate
  thirtyMinBreaks: number
  tenMinBreaks: number
  repeating: boolean
  daysPerRepeat: number
  first_name: string
  last_name: string
  nickname: string
  role: string
  profileBG: string
  profileText: string
  originalDate: ISODate | null
}

export interface ShiftSegment { startTime: ISOTime; endTime: ISOTime; onLeave: boolean }
export interface SpecialDate { eventID: number; eventDate: ISODate; storeStatus: string; note: string }
export interface LeaveRequest {
  leaveID: number
  storeID: number
  userID: number
  employeeFirstName: string
  employeeLastName: string
  employeeRole: string
  fromDate: ISODateTime
  toDate: ISODateTime
  leaveType: string
  leaveReason: string
}

export interface BASCheck {
  date: ISODate
  storeID: number
  cashAdjustment: number
  eftposAdjustment: number
  amexAdjustment: number
  googleSquareAdjustment: number
  chequeAdjustment: number
  medicareAdjustment: number
  totalIncomeAdjustment: number
  cashCorrect: boolean
  eftposCorrect: boolean
  amexCorrect: boolean
  googleSquareCorrect: boolean
  chequeCorrect: boolean
  medicareCorrect: boolean
  totalIncomeCorrect: boolean
  gstCorrect: boolean
  basDailyScript: number
}

export interface BudgetExpenses {
  date: ISODate
  storeID: number
  monthlyRent: number
  dailyOutgoings: number
  buildingOutgoings: number
  monthlyLoan: number
  cpaIncome: number
  lanternIncome: number
  otherIncome: number
  atoGSTrefund: number
  monthlyWages: number
}

export interface MonthlySummary {
  date: ISODate
  dayDuration: number
  noOfScripts: number
  noOfCustomers: number
  noOfItems: number
  noOfOTCItems: number
  itemsPerCustomer: number
  otcPerCustomer: number
  dollarPerCustomer: number
  otcDollarPerCustomer: number
  totalIncome: number
  gpDollars: number
  gpPercentage: number
  rentAndOutgoings: number
  wages: number
  outgoings: number
  zReportProfit: number
  runningZProfit: number
  tillBalance: number
  runningTillBalance: number
  grossProfitDollars: number
  govtRecovery: number
  totalGovtContribution: number
}

export interface CellDataPoint {
  assignedDate: ISODate | null
  category: string
  subCategory: string
  quantity: number
  amount: number
}
