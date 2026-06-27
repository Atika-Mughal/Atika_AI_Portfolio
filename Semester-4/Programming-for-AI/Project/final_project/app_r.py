import streamlit as st
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from matplotlib import rcParams

from rdkit import Chem, RDLogger
from rdkit.Chem import Draw, Descriptors, Crippen, Lipinski, rdMolDescriptors
from rdkit.Chem.rdFingerprintGenerator import GetMorganGenerator

from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.svm import SVC
from sklearn.metrics import accuracy_score, f1_score, confusion_matrix

import io
from datetime import datetime
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.units import cm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Image as RLImage,
    Table, TableStyle, PageBreak
)

RDLogger.DisableLog('rdApp.*')


# PAGE CONFIG

st.set_page_config(
    page_title="Drug Toxicity Prediction Lab",
    page_icon="🧪",
    layout="wide",
    initial_sidebar_state="expanded",
)


st.markdown("""
<style>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@600;700;800&family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap');

:root {
    --bg-1: #0b1224;
    --bg-2: #0f172a;
    --bg-3: #131c36;
    --surface: rgba(255,255,255,0.06);
    --surface-strong: rgba(255,255,255,0.10);
    --surface-solid: #182447;
    --border: rgba(212,175,110,0.22);
    --border-strong: rgba(212,175,110,0.55);
    --text: #f1f5ff;
    --text-dim: #c8d3ec;
    --muted: #8b97b8;
    --gold: #d4af6e;
    --gold-bright: #f1cf86;
    --gold-deep: #a8854b;
    --teal: #5eead4;
    --teal-deep: #14b8a6;
    --indigo: #818cf8;
    --toxic: #ff6b7a;
    --toxic-bg: rgba(220,38,38,0.14);
    --safe: #34d399;
    --safe-bg: rgba(16,185,129,0.14);
    --warn: #fbbf24;
    --warn-bg: rgba(251,191,36,0.12);
}

/* === Streamlit native header bar === */
header[data-testid="stHeader"],
header[data-testid="stHeader"] > div,
.stAppHeader,
.stAppHeader > div,
[data-testid="stToolbar"],
[data-testid="stStatusWidget"] {
    background: #0b1224 !important;
    background-color: #0b1224 !important;
    border-bottom: 1px solid rgba(212,175,110,0.15) !important;
}
/* Hide the white decoration stripe Streamlit injects at top */
[data-testid="stDecoration"] {
    display: none !important;
}

/* Global */
html, body, [class*="css"], .stApp, .main, .block-container {
    font-family: 'Inter', -apple-system, sans-serif !important;
    color: var(--text) !important;
}
.stApp {
    background:
        radial-gradient(1100px 600px at 8% -10%, rgba(212,175,110,0.18), transparent 60%),
        radial-gradient(900px 500px at 100% 5%, rgba(94,234,212,0.12), transparent 60%),
        radial-gradient(700px 500px at 50% 110%, rgba(129,140,248,0.10), transparent 60%),
        linear-gradient(180deg, var(--bg-1) 0%, var(--bg-2) 60%, var(--bg-3) 100%) !important;
    background-attachment: fixed !important;
}

/* Text readability everywhere */
p, span, label, li, div, small, .stMarkdown, .stCaption, .stText {
    color: var(--text) !important;
}
.stCaption, [data-testid="stCaptionContainer"] { color: var(--text-dim) !important; }
h1, h2, h3, h4, h5, h6 {
    color: var(--text) !important;
    font-weight: 700;
    letter-spacing: -0.01em;
}

/* === HEADER  === */
.lab-header {
    position: relative;
    background:
        linear-gradient(135deg, rgba(212,175,110,0.12), rgba(94,234,212,0.06) 50%, rgba(129,140,248,0.10)),
        linear-gradient(160deg, #111a36 0%, #0c1430 100%);
    padding: 2.4rem 2.4rem;
    border-radius: 22px;
    color: white;
    margin-bottom: 1rem;
    box-shadow:
        0 20px 60px rgba(0,0,0,0.45),
        0 8px 24px rgba(212,175,110,0.18),
        inset 0 1px 0 rgba(255,255,255,0.10),
        inset 0 -1px 0 rgba(0,0,0,0.4);
    border: 1px solid var(--border);
    overflow: hidden;
    transform: perspective(1200px) rotateX(0deg);
    transition: transform 0.4s ease;
}
.lab-header:hover { transform: perspective(1200px) rotateX(0.6deg) translateY(-2px); }
.lab-header::before {
    content: ""; position: absolute; inset: 0;
    background:
        repeating-linear-gradient(45deg, rgba(255,255,255,0.02) 0 2px, transparent 2px 14px);
    pointer-events: none;
}
.lab-header::after {
    content: ""; position: absolute; right: -60px; top: -60px;
    width: 280px; height: 280px; border-radius: 50%;
    background: radial-gradient(circle, rgba(212,175,110,0.30), transparent 70%);
    filter: blur(2px);
}
.lab-header h1 {
    margin: 0; font-size: 2.2rem; font-weight: 800;
    font-family: 'Playfair Display', serif;
    color: #fff !important;
    text-shadow: 0 2px 18px rgba(0,0,0,0.5);
    background: linear-gradient(120deg, #fff 30%, var(--gold-bright) 70%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    background-clip: text;
}
.lab-header p {
    margin: 0.6rem 0 0; opacity: 0.95; font-size: 0.95rem;
    font-family: 'JetBrains Mono', monospace; color: #e7eefc !important;
    letter-spacing: 0.02em;
}
.lab-header .pill {
    display: inline-block; margin-top: 0.85rem; padding: 0.3rem 0.85rem;
    background: rgba(212,175,110,0.18); color: var(--gold-bright) !important;
    border: 1px solid rgba(212,175,110,0.45); border-radius: 999px;
    font-family: 'JetBrains Mono', monospace; font-size: 0.72rem; font-weight: 700;
    letter-spacing: 0.12em; text-transform: uppercase;
    box-shadow: 0 4px 14px rgba(212,175,110,0.20), inset 0 1px 0 rgba(255,255,255,0.10);
}

/* === Disclaimer === */
.disclaimer {
    background: linear-gradient(95deg, rgba(251,191,36,0.10), rgba(220,38,38,0.08));
    border: 1px solid rgba(251,191,36,0.35);
    border-left: 4px solid var(--warn);
    border-radius: 14px;
    padding: 1rem 1.25rem;
    margin-bottom: 1.25rem;
    color: #fef3c7 !important;
    backdrop-filter: blur(10px);
    box-shadow: 0 6px 20px rgba(0,0,0,0.25);
}
.disclaimer strong { color: var(--gold-bright) !important; font-weight: 700; }
.disclaimer .small { font-size: 0.82rem; color: #fde68a !important; display: block; margin-top: 0.35rem; opacity: 0.9; }

/* === Glass cards  === */
.lab-card {
    position: relative;
    background:
        linear-gradient(160deg, rgba(255,255,255,0.07), rgba(255,255,255,0.02));
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 1.15rem 1.35rem;
    box-shadow:
        0 10px 30px rgba(0,0,0,0.35),
        inset 0 1px 0 rgba(255,255,255,0.06);
    margin-bottom: 1rem;
    backdrop-filter: blur(14px) saturate(140%);
    -webkit-backdrop-filter: blur(14px) saturate(140%);
    transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}
.lab-card::before {
    content: ""; position: absolute; inset: 0;
    border-radius: 16px;
    background: linear-gradient(120deg, rgba(212,175,110,0.05), transparent 40%);
    pointer-events: none;
}
.lab-card:hover {
    transform: translateY(-4px) scale(1.008);
    border-color: var(--border-strong);
    box-shadow:
        0 18px 40px rgba(0,0,0,0.45),
        0 6px 20px rgba(212,175,110,0.22),
        inset 0 1px 0 rgba(255,255,255,0.10);
}
.lab-card h3 {
    margin: 0 0 0.55rem !important; font-size: 0.7rem !important; font-weight: 700 !important;
    text-transform: uppercase; letter-spacing: 0.16em;
    color: var(--gold) !important;
    font-family: 'JetBrains Mono', monospace;
}
.lab-metric {
    font-family: 'JetBrains Mono', monospace;
    font-size: 2.1rem; font-weight: 800;
    line-height: 1.1;
    background: linear-gradient(135deg, var(--gold-bright) 0%, var(--teal) 100%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    background-clip: text;
    color: var(--gold-bright) !important;
    text-shadow: 0 2px 12px rgba(212,175,110,0.20);
}

/* === Workflow strip === */
.workflow-strip {
    display: flex; gap: 0.4rem; flex-wrap: wrap; align-items: center;
    background: linear-gradient(90deg, rgba(212,175,110,0.07), rgba(94,234,212,0.06));
    border: 1px solid var(--border);
    padding: 0.9rem 1.3rem; border-radius: 14px;
    margin-bottom: 1.25rem;
    font-family: 'JetBrains Mono', monospace; font-size: 0.85rem;
    color: var(--gold-bright) !important;
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.05);
}
.workflow-strip span { white-space: nowrap; color: var(--gold-bright) !important; }
.workflow-strip span + span::before { content: "→"; margin: 0 0.55rem; color: var(--teal); }

/* === Tabs === */
.stTabs [data-baseweb="tab-list"] {
    gap: 0.35rem;
    background: rgba(255,255,255,0.04);
    padding: 0.5rem; border-radius: 14px;
    border: 1px solid var(--border);
    backdrop-filter: blur(10px);
}
.stTabs [data-baseweb="tab"] {
    background: transparent !important; border-radius: 10px;
    padding: 0.6rem 1.3rem; font-weight: 600; color: var(--text-dim) !important;
    transition: all 0.2s ease;
}
.stTabs [data-baseweb="tab"]:hover {
    color: var(--gold-bright) !important;
    background: rgba(212,175,110,0.08) !important;
    transform: translateY(-1px);
}
.stTabs [aria-selected="true"] {
    background: linear-gradient(135deg, var(--gold-deep), var(--gold)) !important;
    color: #1a1206 !important;
    box-shadow: 0 6px 18px rgba(212,175,110,0.40), inset 0 1px 0 rgba(255,255,255,0.30);
    font-weight: 700 !important;
}

/* === Buttons — 3D gold === */
.stButton > button, .stDownloadButton > button {
    background: linear-gradient(135deg, var(--gold-deep) 0%, var(--gold-bright) 100%) !important;
    color: #1a1206 !important; border: none !important;
    padding: 0.65rem 1.5rem !important; font-weight: 800 !important; border-radius: 12px !important;
    transition: all 0.22s ease !important; font-family: 'Inter', sans-serif !important;
    box-shadow:
        0 8px 22px rgba(212,175,110,0.35),
        inset 0 1px 0 rgba(255,255,255,0.45),
        inset 0 -2px 0 rgba(0,0,0,0.18) !important;
    letter-spacing: 0.02em;
    text-shadow: 0 1px 0 rgba(255,255,255,0.25);
}
.stButton > button:hover, .stDownloadButton > button:hover {
    transform: translateY(-3px) !important;
    box-shadow:
        0 14px 30px rgba(212,175,110,0.50),
        inset 0 1px 0 rgba(255,255,255,0.55),
        inset 0 -2px 0 rgba(0,0,0,0.20) !important;
    filter: brightness(1.06);
}
.stButton > button:active { transform: translateY(-1px) !important; }

/* === Inputs === */
.stTextInput input, .stTextArea textarea {
    font-family: 'JetBrains Mono', monospace !important;
    background: rgba(255,255,255,0.06) !important;
    color: var(--text) !important;
    border: 1px solid var(--border) !important;
    border-radius: 12px !important;
    padding: 0.7rem 1rem !important;
    box-shadow: inset 0 2px 6px rgba(0,0,0,0.25);
    transition: all 0.2s ease;
}
.stTextInput input::placeholder { color: var(--muted) !important; opacity: 1; }
.stTextInput input:focus, .stTextArea textarea:focus {
    border-color: var(--gold) !important;
    box-shadow:
        0 0 0 3px rgba(212,175,110,0.22),
        inset 0 2px 6px rgba(0,0,0,0.25) !important;
    background: rgba(255,255,255,0.09) !important;
}

/* === Selectbox === */
.stSelectbox > div > div, [data-baseweb="select"] > div {
    background: rgba(24, 36, 71, 0.95) !important;
    color: var(--text) !important;
    border: 1px solid var(--border) !important;
    border-radius: 12px !important;
}
.stSelectbox label, .stTextInput label { color: var(--gold-bright) !important; font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; font-size: 0.72rem; }
[data-baseweb="select"] * { color: var(--text) !important; background: transparent; }
/* Dropdown popover — fully opaque dark background */
[data-baseweb="popover"],
[data-baseweb="popover"] > div,
[data-baseweb="popover"] ul,
[data-baseweb="menu"],
[data-baseweb="menu"] > ul {
    background: var(--surface-solid) !important;
    background-color: #182447 !important;
    border: 1px solid var(--border) !important;
    border-radius: 10px !important;
    box-shadow: 0 12px 32px rgba(0,0,0,0.55) !important;
}
[data-baseweb="popover"] li,
[data-baseweb="menu"] li,
[data-baseweb="option"] {
    background: #182447 !important;
    background-color: #182447 !important;
    color: var(--text) !important;
}
[data-baseweb="popover"] li:hover,
[data-baseweb="menu"] li:hover,
[data-baseweb="option"]:hover {
    background: rgba(212,175,110,0.18) !important;
    background-color: rgba(212,175,110,0.18) !important;
    color: var(--gold-bright) !important;
}

/* Result badges with glow*/
.badge-toxic {
    display: inline-block; padding: 0.7rem 1.6rem;
    background: var(--toxic-bg); color: var(--toxic) !important;
    border: 1px solid rgba(255,107,122,0.55); border-radius: 12px;
    font-weight: 800; font-size: 1.2rem;
    font-family: 'JetBrains Mono', monospace;
    box-shadow: 0 6px 22px rgba(255,107,122,0.30), inset 0 1px 0 rgba(255,255,255,0.10);
    text-shadow: 0 0 14px rgba(255,107,122,0.55);
    animation: pulseGlow 2.4s ease-in-out infinite;
}
.badge-safe {
    display: inline-block; padding: 0.7rem 1.6rem;
    background: var(--safe-bg); color: var(--safe) !important;
    border: 1px solid rgba(52,211,153,0.55); border-radius: 12px;
    font-weight: 800; font-size: 1.2rem;
    font-family: 'JetBrains Mono', monospace;
    box-shadow: 0 6px 22px rgba(52,211,153,0.30), inset 0 1px 0 rgba(255,255,255,0.10);
    text-shadow: 0 0 14px rgba(52,211,153,0.55);
    animation: pulseGlow 2.4s ease-in-out infinite;
}
@keyframes pulseGlow {
    0%,100% { filter: brightness(1); }
    50% { filter: brightness(1.12); }
}

/* === Sidebar === */
[data-testid="stSidebar"] {
    background: linear-gradient(180deg, #0a1024 0%, #0e1530 100%) !important;
    border-right: 1px solid var(--border);
    box-shadow: inset -1px 0 0 rgba(212,175,110,0.10);
}
[data-testid="stSidebar"] * { color: var(--text) !important; }
[data-testid="stSidebar"] h2, [data-testid="stSidebar"] h3 {
    color: var(--gold-bright) !important;
    font-family: 'Playfair Display', serif;
}
[data-testid="stSidebar"] code, [data-testid="stSidebar"] pre {
    background: rgba(255,255,255,0.05) !important;
    color: var(--teal) !important;
    border: 1px solid var(--border) !important; border-radius: 10px !important;
}

/* Code blocks */
code, pre {
    background: rgba(255,255,255,0.05) !important;
    color: var(--teal) !important;
    border-radius: 8px;
}
pre { border: 1px solid var(--border) !important; padding: 0.7rem !important; }

/* === Dataframe === */
.stDataFrame, .stDataFrame * { color: var(--text) !important; }
.stDataFrame {
    border: 1px solid var(--border);
    border-radius: 12px; overflow: hidden;
    box-shadow: 0 8px 22px rgba(0,0,0,0.35);
}
[data-testid="stDataFrame"] div[role="grid"] { background: rgba(255,255,255,0.03) !important; }
[data-testid="stDataFrame"] [role="columnheader"] {
    background: rgba(212,175,110,0.12) !important; color: var(--gold-bright) !important;
    font-weight: 700 !important;
}

/* Progress */
.stProgress > div > div > div > div {
    background: linear-gradient(90deg, var(--gold), var(--teal)) !important;
    box-shadow: 0 0 12px rgba(212,175,110,0.45);
}
.stProgress > div > div { background: rgba(255,255,255,0.08) !important; }

/* Alerts */
.stAlert { border-radius: 12px; border-left-width: 4px; backdrop-filter: blur(8px); }
.stAlert [data-testid="stMarkdownContainer"] * { color: var(--text) !important; }
div[data-baseweb="notification"] { background: var(--surface-strong) !important; }

.stSuccess { background: rgba(16,185,129,0.12) !important; border-color: var(--safe) !important; }
.stWarning { background: rgba(251,191,36,0.12) !important; border-color: var(--warn) !important; }
.stError   { background: rgba(220,38,38,0.14) !important; border-color: var(--toxic) !important; }
.stInfo    { background: rgba(94,234,212,0.10) !important; border-color: var(--teal) !important; }

/* Spinner */
.stSpinner > div > div { color: var(--gold-bright) !important; }

/* Image polish — molecule renders */
.stImage > img {
    background: #fff;
    border-radius: 14px;
    border: 1px solid var(--border);
    padding: 8px;
    box-shadow: 0 12px 28px rgba(0,0,0,0.45), inset 0 1px 0 rgba(255,255,255,0.6);
    transition: transform 0.3s ease;
}
.stImage > img:hover { transform: translateY(-3px) scale(1.01); }

/* Footer */
.footer-note {
    margin-top: 2rem; padding: 1.1rem 1.3rem;
    background: rgba(255,255,255,0.04);
    border: 1px dashed var(--border);
    border-radius: 12px;
    text-align: center;
    color: var(--text-dim) !important;
    font-size: 0.84rem;
    backdrop-filter: blur(8px);
}
.footer-note strong { color: var(--gold-bright) !important; }
</style>
""", unsafe_allow_html=True)

# Matplotlib dark theme aligned with palette
rcParams['font.family'] = 'sans-serif'
rcParams['axes.facecolor'] = '#131c36'
rcParams['figure.facecolor'] = '#0f172a'
rcParams['savefig.facecolor'] = '#0f172a'
rcParams['axes.edgecolor'] = '#3b4a78'
rcParams['axes.labelcolor'] = '#f1f5ff'
rcParams['xtick.color'] = '#c8d3ec'
rcParams['ytick.color'] = '#c8d3ec'
rcParams['text.color'] = '#f1f5ff'
rcParams['axes.spines.top'] = False
rcParams['axes.spines.right'] = False
rcParams['axes.grid'] = True
rcParams['grid.color'] = '#243259'
rcParams['grid.alpha'] = 0.6


# SESSION STATE

if "history" not in st.session_state:
    st.session_state["history"] = []
if "last_prediction" not in st.session_state:
    st.session_state["last_prediction"] = None


# FEATURE GENERATOR

morgan_gen = GetMorganGenerator(radius=2, fpSize=1024)

def featurize(smiles):
    mol = Chem.MolFromSmiles(smiles)
    if mol is None:
        return None
    return list(morgan_gen.GetFingerprint(mol))

def label_map(val):
    return "Toxic" if val == 1 else "Non-Toxic"

def cm_to_list_safe(cm_arr):
    try:
        return [[int(x) for x in row] for row in cm_arr]
    except Exception:
        return []

def compute_descriptors(mol):
    """Compute classical molecular descriptors used in drug design."""
    try:
        return {
            "Molecular Formula": rdMolDescriptors.CalcMolFormula(mol),
            "Molecular Weight (g/mol)": f"{Descriptors.MolWt(mol):.2f}",
            "Heavy Atoms": int(mol.GetNumHeavyAtoms()),
            "Atoms (incl. H)": int(Chem.AddHs(mol).GetNumAtoms()),
            "Bonds": int(mol.GetNumBonds()),
            "Rings": int(rdMolDescriptors.CalcNumRings(mol)),
            "Aromatic Rings": int(rdMolDescriptors.CalcNumAromaticRings(mol)),
            "Rotatable Bonds": int(Lipinski.NumRotatableBonds(mol)),
            "H-Bond Donors": int(Lipinski.NumHDonors(mol)),
            "H-Bond Acceptors": int(Lipinski.NumHAcceptors(mol)),
            "TPSA (Å²)": f"{Descriptors.TPSA(mol):.2f}",
            "LogP (Crippen)": f"{Crippen.MolLogP(mol):.3f}",
            "Molar Refractivity": f"{Crippen.MolMR(mol):.2f}",
            "Formal Charge": int(Chem.GetFormalCharge(mol)),
        }
    except Exception:
        return {}

def lipinski_assessment(mol):
    """Lipinski Rule of Five quick check."""
    mw = Descriptors.MolWt(mol)
    logp = Crippen.MolLogP(mol)
    hbd = Lipinski.NumHDonors(mol)
    hba = Lipinski.NumHAcceptors(mol)
    rules = [
        ("MW ≤ 500", mw <= 500, f"{mw:.2f}"),
        ("LogP ≤ 5", logp <= 5, f"{logp:.3f}"),
        ("H-Bond Donors ≤ 5", hbd <= 5, str(hbd)),
        ("H-Bond Acceptors ≤ 10", hba <= 10, str(hba)),
    ]
    violations = sum(1 for _, ok, _ in rules if not ok)
    return rules, violations

def build_material_pdf_report(payload: dict) -> bytes:
    """
    Build a PDF report for a SINGLE predicted material with all its details:
    SMILES, structure, prediction, confidence, class probabilities,
    molecular descriptors, Lipinski check, model used, timestamp, disclaimer.
    """
    buf = io.BytesIO()
    doc = SimpleDocTemplate(
        buf, pagesize=A4,
        leftMargin=1.8*cm, rightMargin=1.8*cm,
        topMargin=1.6*cm, bottomMargin=1.6*cm,
        title=f"Toxicity Report — {payload.get('smiles','molecule')}",
    )
    styles = getSampleStyleSheet()
    h1 = ParagraphStyle("h1", parent=styles["Heading1"], fontSize=20,
                        textColor=colors.HexColor("#a8854b"), spaceAfter=4)
    h2 = ParagraphStyle("h2", parent=styles["Heading2"], fontSize=13,
                        textColor=colors.HexColor("#0f172a"), spaceBefore=14, spaceAfter=6)
    body = ParagraphStyle("body", parent=styles["BodyText"], fontSize=10.5,
                          leading=15, textColor=colors.HexColor("#0f172a"))
    small = ParagraphStyle("small", parent=styles["BodyText"], fontSize=8.5,
                           leading=12, textColor=colors.HexColor("#475569"))
    mono = ParagraphStyle("mono", parent=styles["BodyText"], fontSize=10,
                          leading=14, fontName="Courier",
                          textColor=colors.HexColor("#0f172a"))
    disclaimer_style = ParagraphStyle(
        "disc", parent=styles["BodyText"], fontSize=10, leading=14,
        textColor=colors.HexColor("#78350f"),
        backColor=colors.HexColor("#fef3c7"),
        borderColor=colors.HexColor("#b45309"), borderWidth=1, borderPadding=8,
        leftIndent=0, rightIndent=0, spaceBefore=4, spaceAfter=10,
    )

    story = []
    story.append(Paragraph("🧪 Material Toxicity Report", h1))
    story.append(Paragraph(
        f"Generated: {payload.get('timestamp','—')} &nbsp;·&nbsp; Model: <b>{payload.get('model_name','—')}</b>",
        small
    ))
    story.append(Spacer(1, 8))

    # Disclaimer
    story.append(Paragraph(
        "<b>⚠ Scientific Disclaimer:</b> This report is generated by an AI/ML model for "
        "educational and research purposes only. The prediction is a <b>probabilistic estimate</b> "
        "and is <b>not 100% accurate</b>. It must <b>not</b> be used for clinical, medical, "
        "diagnostic, or regulatory decisions. Always verify findings through "
        "<b>certified scientific laboratories</b>, in-vitro / in-vivo assays, and peer-reviewed "
        "toxicological studies before any real-world application.",
        disclaimer_style
    ))

    # ===== Identity =====
    story.append(Paragraph("Material Identity", h2))
    identity = [
        ["SMILES", payload.get("smiles", "—")],
        ["Molecular Formula", payload.get("descriptors", {}).get("Molecular Formula", "—")],
        ["Canonical SMILES", payload.get("canonical_smiles", "—")],
        ["InChIKey", payload.get("inchikey", "—")],
    ]
    t = Table(identity, colWidths=[4.5*cm, 11.5*cm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0,0), (0,-1), colors.HexColor("#f5f1e6")),
        ("TEXTCOLOR", (0,0), (-1,-1), colors.HexColor("#0f172a")),
        ("FONTNAME", (0,0), (0,-1), "Helvetica-Bold"),
        ("FONTNAME", (1,0), (1,-1), "Courier"),
        ("FONTSIZE", (0,0), (-1,-1), 9.5),
        ("BOTTOMPADDING", (0,0), (-1,-1), 6),
        ("TOPPADDING", (0,0), (-1,-1), 6),
        ("GRID", (0,0), (-1,-1), 0.4, colors.HexColor("#d6cfb8")),
        ("VALIGN", (0,0), (-1,-1), "MIDDLE"),
    ]))
    story.append(t)

    # ===== Structure image + Prediction side by side =====
    story.append(Paragraph("Structure & Prediction", h2))

    pred_label = payload.get("prediction", "—")
    confidence_pct = payload.get("confidence_pct", None)
    pred_color = colors.HexColor("#dc2626") if pred_label == "Toxic" else colors.HexColor("#047857")

    # Structure
    img_bytes = payload.get("structure_png")
    if img_bytes:
        struct_img = RLImage(io.BytesIO(img_bytes), width=7*cm, height=7*cm)
    else:
        struct_img = Paragraph("(structure unavailable)", small)

    # Prediction summary block as a nested table
    pred_rows = [
        ["Prediction", pred_label],
        ["Confidence", f"{confidence_pct:.2f}%" if confidence_pct is not None else "—"],
        ["P(Non-Toxic)", f"{payload.get('p_non_toxic', 0)*100:.2f}%"],
        ["P(Toxic)", f"{payload.get('p_toxic', 0)*100:.2f}%"],
        ["Model", payload.get("model_name", "—")],
        ["Trained at", payload.get("model_trained_at", "—")],
    ]
    pred_tbl = Table(pred_rows, colWidths=[3.8*cm, 4.4*cm])
    pred_tbl.setStyle(TableStyle([
        ("BACKGROUND", (0,0), (0,-1), colors.HexColor("#f5f1e6")),
        ("FONTNAME", (0,0), (0,-1), "Helvetica-Bold"),
        ("FONTSIZE", (0,0), (-1,-1), 10),
        ("BOTTOMPADDING", (0,0), (-1,-1), 5),
        ("TOPPADDING", (0,0), (-1,-1), 5),
        ("GRID", (0,0), (-1,-1), 0.4, colors.HexColor("#d6cfb8")),
        ("TEXTCOLOR", (1,0), (1,0), pred_color),
        ("FONTNAME", (1,0), (1,0), "Helvetica-Bold"),
    ]))

    combo = Table([[struct_img, pred_tbl]], colWidths=[7.5*cm, 8.5*cm])
    combo.setStyle(TableStyle([
        ("VALIGN", (0,0), (-1,-1), "TOP"),
        ("LEFTPADDING", (0,0), (-1,-1), 0),
        ("RIGHTPADDING", (0,0), (-1,-1), 0),
    ]))
    story.append(combo)

    # ===== Molecular descriptors =====
    story.append(Paragraph("Molecular Descriptors", h2))
    desc = payload.get("descriptors", {})
    if desc:
        items = list(desc.items())
        # 2-column grid
        rows = []
        for i in range(0, len(items), 2):
            left = items[i]
            right = items[i+1] if i+1 < len(items) else ("", "")
            rows.append([left[0], str(left[1]), right[0], str(right[1])])
        dt = Table(rows, colWidths=[4.2*cm, 3.8*cm, 4.2*cm, 3.8*cm])
        dt.setStyle(TableStyle([
            ("BACKGROUND", (0,0), (0,-1), colors.HexColor("#f5f1e6")),
            ("BACKGROUND", (2,0), (2,-1), colors.HexColor("#f5f1e6")),
            ("FONTNAME", (0,0), (0,-1), "Helvetica-Bold"),
            ("FONTNAME", (2,0), (2,-1), "Helvetica-Bold"),
            ("FONTSIZE", (0,0), (-1,-1), 9.5),
            ("BOTTOMPADDING", (0,0), (-1,-1), 5),
            ("TOPPADDING", (0,0), (-1,-1), 5),
            ("GRID", (0,0), (-1,-1), 0.3, colors.HexColor("#d6cfb8")),
        ]))
        story.append(dt)
    else:
        story.append(Paragraph("Descriptors unavailable.", body))

    # ===== Lipinski =====
    story.append(Paragraph("Lipinski's Rule of Five", h2))
    lip = payload.get("lipinski", [])
    if lip:
        rows = [["Rule", "Value", "Pass"]]
        for rule, ok, val in lip:
            rows.append([rule, val, "✓" if ok else "✗"])
        lt = Table(rows, colWidths=[7*cm, 5*cm, 4*cm])
        lt.setStyle(TableStyle([
            ("BACKGROUND", (0,0), (-1,0), colors.HexColor("#a8854b")),
            ("TEXTCOLOR", (0,0), (-1,0), colors.white),
            ("FONTNAME", (0,0), (-1,0), "Helvetica-Bold"),
            ("FONTSIZE", (0,0), (-1,-1), 10),
            ("ALIGN", (2,1), (2,-1), "CENTER"),
            ("GRID", (0,0), (-1,-1), 0.3, colors.HexColor("#d6cfb8")),
            ("BOTTOMPADDING", (0,0), (-1,-1), 5),
            ("TOPPADDING", (0,0), (-1,-1), 5),
            ("ROWBACKGROUNDS", (0,1), (-1,-1), [colors.white, colors.HexColor("#faf6ea")]),
        ]))
        # color pass/fail
        for r, (_, ok, _) in enumerate(lip, start=1):
            color = colors.HexColor("#047857") if ok else colors.HexColor("#dc2626")
            lt.setStyle(TableStyle([("TEXTCOLOR", (2, r), (2, r), color),
                                    ("FONTNAME", (2, r), (2, r), "Helvetica-Bold")]))
        story.append(lt)
        viol = payload.get("lipinski_violations", 0)
        story.append(Spacer(1, 6))
        story.append(Paragraph(f"<b>Violations:</b> {viol} / 4", body))

    # ===== Footer disclaimer =====
    story.append(Spacer(1, 16))
    story.append(Paragraph(
        "<b>Reminder:</b> AI predictions are probabilistic estimates — not laboratory-certified results. "
        "Always confirm toxicity findings through accredited scientific laboratories before any real-world use.",
        small
    ))

    doc.build(story)
    buf.seek(0)
    return buf.getvalue()

# =========================================================
# HEADER
# =========================================================
st.markdown("""
<div class="lab-header">
    <h1>🧪 AI Drug Toxicity Prediction Lab</h1>
    <p>Molecular fingerprinting · Machine learning · Confidence scoring</p>
    <span class="pill">⚡ Research Prototype</span>
</div>
""", unsafe_allow_html=True)

st.markdown("""
<div class="disclaimer">
    ⚠️ <strong>Scientific Disclaimer:</strong> This tool provides AI-based predictions for educational
    and research purposes only. Results are <strong>not 100% accurate</strong> and must
    <strong>not</strong> be used for clinical, medical, or regulatory decisions.
    Always validate findings through certified scientific laboratories, in-vitro/in-vivo assays,
    and peer-reviewed toxicological studies before drawing any conclusions.
    <span class="small">Model: Tox21 dataset · Morgan fingerprints (ECFP4) · No substitute for wet-lab verification.</span>
</div>
""", unsafe_allow_html=True)

st.markdown("""
<div class="workflow-strip">
    <span>① SMILES Input</span>
    <span>② RDKit Featurize</span>
    <span>③ Train Model</span>
    <span>④ Predict</span>
    <span>⑤ Confidence</span>
    <span>⑥ PDF Report</span>
</div>
""", unsafe_allow_html=True)

# =========================================================
# SIDEBAR
# =========================================================
with st.sidebar:
    st.markdown("## 🔬 Lab Console")
    st.markdown("---")
    st.markdown("**Fingerprint**")
    st.code("Morgan (ECFP4)\nradius = 2\nbits   = 1024", language="yaml")
    st.markdown("**Status**")
    if "model" in st.session_state:
        st.success("✓ Model loaded")
    else:
        st.warning("○ No model trained")
    st.markdown(f"**Predictions logged:** `{len(st.session_state['history'])}`")
    st.markdown("---")
    st.markdown("**💡 Try these SMILES**")
    st.code("CCO            # ethanol\nc1ccccc1       # benzene\nCC(=O)Oc1ccccc1C(=O)O  # aspirin\nCOC1=CC=C(C(=O)C2=CC=CC=C2)C(O)=C1    # oxybenzone", language="text")
    st.markdown("---")
    st.caption("⚠ Predictions are not a substitute for laboratory verification.")

# =========================================================
# LOAD DATA
# =========================================================
@st.cache_data
def load_data():
    return pd.read_csv("tox21_train_clean.csv"), pd.read_csv("ktest.csv")

try:
    train_df, test_df = load_data()
except FileNotFoundError as e:
    st.error(f"Missing data file: {e}. Place `tox21_train_clean.csv` and `ktest.csv` next to app_r.py.")
    st.stop()

# =========================================================
# TABS
# =========================================================
tab1, tab2, tab3 = st.tabs(["📊  Training", "🔍  Prediction", "📜  History"])

# =========================================================
# TRAINING TAB
# =========================================================
with tab1:
    col_a, col_b = st.columns([2, 1])
    with col_a:
        st.markdown("### Model Training & Evaluation")
        st.caption("Train a classifier on the Tox21 dataset and evaluate on the holdout set.")
    with col_b:
        model_name = st.selectbox(
            "Select Model",
            ["Random Forest", "Logistic Regression", "SVM"]
        )

    c1, c2, c3 = st.columns(3)
    with c1:
        st.markdown(f'<div class="lab-card"><h3>Train Compounds</h3><div class="lab-metric">{len(train_df):,}</div></div>', unsafe_allow_html=True)
    with c2:
        st.markdown(f'<div class="lab-card"><h3>Test Compounds</h3><div class="lab-metric">{len(test_df):,}</div></div>', unsafe_allow_html=True)
    with c3:
        st.markdown(f'<div class="lab-card"><h3>Feature Bits</h3><div class="lab-metric">1024</div></div>', unsafe_allow_html=True)

    if st.button("⚗️  Train & Evaluate", key="train_btn"):
        with st.spinner("Featurizing molecules and training model..."):
            train_feat = train_df["smiles"].apply(featurize)
            train_df_clean = train_df[train_feat.notnull()]
            X_train = list(train_feat.dropna())
            y_train = train_df_clean.loc[train_feat.notnull(), "toxicity"]

            test_feat = test_df["smiles"].apply(featurize)
            test_df_clean = test_df[test_feat.notnull()]
            X_test = list(test_feat.dropna())
            y_test = test_df_clean.loc[test_feat.notnull(), "toxicity"]

            if model_name == "Random Forest":
                model = RandomForestClassifier()
            elif model_name == "Logistic Regression":
                model = LogisticRegression(max_iter=1000)
            else:
                model = SVC(probability=True)

            model.fit(X_train, y_train)
            y_pred = model.predict(X_test)

            acc = accuracy_score(y_test, y_pred)
            f1 = f1_score(y_test, y_pred)

            st.session_state["model"] = model
            st.session_state["metrics"] = {
                "model_name": model_name,
                "accuracy": float(acc),
                "f1": float(f1),
                "n_train": int(len(X_train)),
                "n_test": int(len(X_test)),
                "trained_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "confusion_matrix": cm_to_list_safe(confusion_matrix(y_test, y_pred)),
            }

        st.success(f"✓ {model_name} trained successfully")

        m1, m2 = st.columns(2)
        with m1:
            st.markdown(f'<div class="lab-card"><h3>Accuracy</h3><div class="lab-metric">{acc:.4f}</div></div>', unsafe_allow_html=True)
        with m2:
            st.markdown(f'<div class="lab-card"><h3>F1 Score</h3><div class="lab-metric">{f1:.4f}</div></div>', unsafe_allow_html=True)

        st.info("ℹ️ These metrics reflect performance on a holdout subset only. Real-world toxicity assessment requires laboratory validation.")

        cm = confusion_matrix(y_test, y_pred)

        fig, axs = plt.subplots(2, 2, figsize=(11, 8))
        gold = '#d4af6e'
        teal = '#5eead4'
        toxic = '#ff6b7a'
        safe = '#34d399'

        axs[0, 0].scatter(range(len(y_test)), y_test, alpha=0.6, s=14, color=teal, edgecolors='none')
        axs[0, 0].set_title("Scatter — Actual Labels", fontsize=11, fontweight='600', loc='left', color='#f1f5ff')

        counts = pd.Series(y_test).value_counts().sort_index()
        axs[0, 1].bar(counts.index.astype(str), counts.values, color=[safe, toxic][:len(counts)], edgecolor='#0f172a')
        axs[0, 1].set_title("Class Distribution", fontsize=11, fontweight='600', loc='left', color='#f1f5ff')

        axs[1, 0].plot(y_test.values[:50], color=gold, linewidth=1.8, marker='o', markersize=4, markerfacecolor=teal)
        axs[1, 0].set_title("Sample Labels Trend (first 50)", fontsize=11, fontweight='600', loc='left', color='#f1f5ff')

        im = axs[1, 1].imshow(cm, cmap="cividis")
        axs[1, 1].set_title("Confusion Matrix", fontsize=11, fontweight='600', loc='left', color='#f1f5ff')
        for i in range(2):
            for j in range(2):
                axs[1, 1].text(j, i, cm[i, j], ha="center", va="center",
                               color="#0f172a" if cm[i, j] > cm.max()/2 else "#f1f5ff",
                               fontweight='800', fontsize=14)
        axs[1, 1].set_xticks([0, 1]); axs[1, 1].set_yticks([0, 1])
        axs[1, 1].set_xticklabels(['Non-Toxic', 'Toxic'])
        axs[1, 1].set_yticklabels(['Non-Toxic', 'Toxic'])
        axs[1, 1].set_xlabel("Predicted"); axs[1, 1].set_ylabel("Actual")
        axs[1, 1].grid(False)

        plt.tight_layout()
        st.pyplot(fig)

        chart_buf = io.BytesIO()
        fig.savefig(chart_buf, format="png", dpi=160, bbox_inches="tight")
        chart_buf.seek(0)
        st.session_state["metrics_chart_png"] = chart_buf.getvalue()

# =========================================================
# PREDICTION TAB
# =========================================================
with tab2:
    st.markdown("### SMILES Prediction")
    st.caption("Enter a SMILES string to predict toxicity using the trained model.")

    col1, col2 = st.columns([3, 1])
    with col1:
        smiles_input = st.text_input("SMILES", placeholder="e.g.  CCO  or  c1ccccc1", label_visibility="collapsed")
    with col2:
        predict_btn = st.button("🔬  Predict", use_container_width=True)

    if predict_btn:
        if "model" not in st.session_state:
            st.error("⚠️  Train a model first in the Training tab.")
        elif not smiles_input.strip():
            st.warning("Enter a SMILES string.")
        else:
            mol = Chem.MolFromSmiles(smiles_input)
            if mol is None:
                st.error("✗ Invalid SMILES string.")
            else:
                features = list(morgan_gen.GetFingerprint(mol))
                model = st.session_state["model"]
                pred = int(model.predict([features])[0])
                label = label_map(pred)

                p_non_toxic = 0.0
                p_toxic = 0.0
                confidence = None
                if hasattr(model, "predict_proba"):
                    probs = model.predict_proba([features])[0]
                    # classes_ may be [0,1] or [1,0]; index safely
                    classes = list(getattr(model, "classes_", [0, 1]))
                    if 0 in classes:
                        p_non_toxic = float(probs[classes.index(0)])
                    if 1 in classes:
                        p_toxic = float(probs[classes.index(1)])
                    confidence = float(max(probs))

                # structure image bytes
                pil_img = Draw.MolToImage(mol, size=(420, 420))
                img_buf = io.BytesIO()
                pil_img.save(img_buf, format="PNG")
                struct_png = img_buf.getvalue()

                # descriptors
                descriptors = compute_descriptors(mol)
                lip_rules, lip_viol = lipinski_assessment(mol)

                try:
                    canonical = Chem.MolToSmiles(mol)
                except Exception:
                    canonical = smiles_input
                try:
                    inchikey = Chem.MolToInchiKey(mol)
                except Exception:
                    inchikey = "—"

                metrics = st.session_state.get("metrics", {})
                payload = {
                    "smiles": smiles_input,
                    "canonical_smiles": canonical,
                    "inchikey": inchikey,
                    "prediction": label,
                    "confidence_pct": (confidence * 100) if confidence is not None else None,
                    "p_non_toxic": p_non_toxic,
                    "p_toxic": p_toxic,
                    "model_name": metrics.get("model_name", "—"),
                    "model_trained_at": metrics.get("trained_at", "—"),
                    "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "descriptors": descriptors,
                    "lipinski": lip_rules,
                    "lipinski_violations": lip_viol,
                    "structure_png": struct_png,
                }
                st.session_state["last_prediction"] = payload

                # === UI ===
                left, right = st.columns([1, 1])
                with left:
                    st.markdown('<div class="lab-card"><h3>Molecular Structure</h3></div>', unsafe_allow_html=True)
                    st.image(pil_img)

                with right:
                    st.markdown('<div class="lab-card"><h3>Prediction</h3>', unsafe_allow_html=True)
                    badge_class = "badge-toxic" if pred == 1 else "badge-safe"
                    icon = "⚠" if pred == 1 else "✓"
                    st.markdown(f'<span class="{badge_class}">{icon}  {label}</span></div>', unsafe_allow_html=True)

                    if confidence is not None:
                        st.markdown('<div class="lab-card"><h3>Confidence</h3>', unsafe_allow_html=True)
                        st.markdown(f'<div class="lab-metric">{confidence*100:.2f}%</div>', unsafe_allow_html=True)
                        st.progress(float(confidence))
                        st.markdown('</div>', unsafe_allow_html=True)

                        st.markdown('<div class="lab-card"><h3>Class Probabilities</h3></div>', unsafe_allow_html=True)
                        prob_df = pd.DataFrame({
                            "Class": ["Non-Toxic", "Toxic"],
                            "Probability": [p_non_toxic, p_toxic]
                        })
                        st.bar_chart(prob_df.set_index("Class"), height=180)

                # Identity + descriptors
                st.markdown('<div class="lab-card"><h3>Material Identity</h3>', unsafe_allow_html=True)
                ident_df = pd.DataFrame({
                    "Field": ["SMILES (input)", "Canonical SMILES", "Molecular Formula", "InChIKey"],
                    "Value": [smiles_input, canonical, descriptors.get("Molecular Formula", "—"), inchikey],
                })
                st.dataframe(ident_df, use_container_width=True, hide_index=True)
                st.markdown('</div>', unsafe_allow_html=True)

                if descriptors:
                    st.markdown('<div class="lab-card"><h3>Molecular Descriptors</h3>', unsafe_allow_html=True)
                    desc_df = pd.DataFrame(
                        [(k, str(v)) for k, v in descriptors.items()],
                        columns=["Descriptor", "Value"],
                    )
                    st.dataframe(desc_df, use_container_width=True, hide_index=True)
                    st.markdown('</div>', unsafe_allow_html=True)

                st.markdown('<div class="lab-card"><h3>Lipinski Rule of Five</h3>', unsafe_allow_html=True)
                lip_df = pd.DataFrame(
                    [(r, v, "✓ Pass" if ok else "✗ Fail") for r, ok, v in lip_rules],
                    columns=["Rule", "Value", "Result"],
                )
                st.dataframe(lip_df, use_container_width=True, hide_index=True)
                st.caption(f"Violations: {lip_viol} / 4")
                st.markdown('</div>', unsafe_allow_html=True)

                # === PDF Report download for this material ===
                pdf_bytes = build_material_pdf_report(payload)
                st.download_button(
                    "📄  Download Report for this Material (PDF)",
                    pdf_bytes,
                    file_name=f"material_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf",
                    mime="application/pdf",
                )

                # log to history
                st.session_state["history"].append({
                    "SMILES": smiles_input,
                    "Prediction": label,
                    "Confidence (%)": round((confidence or 0) * 100, 2)
                })

                st.warning("⚠ This prediction is generated by a machine-learning model and may be incorrect. "
                           "For any safety-critical use, verify with certified laboratory testing.")

    # If user navigates back to this tab and a previous prediction exists,
    # offer the PDF re-download without re-running prediction
    elif st.session_state.get("last_prediction"):
        st.info("Showing the most recent prediction. Run a new prediction above, or download its report below.")
        last = st.session_state["last_prediction"]
        st.markdown(f"**Last SMILES:** `{last['smiles']}` &nbsp;·&nbsp; **Prediction:** {last['prediction']}")
        pdf_bytes = build_material_pdf_report(last)
        st.download_button(
            "📄  Download Last Material Report (PDF)",
            pdf_bytes,
            file_name=f"material_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf",
            mime="application/pdf",
        )

# =========================================================
# HISTORY TAB
# =========================================================
with tab3:
    st.markdown("### Prediction History")
    history = st.session_state["history"]

    if len(history) == 0:
        st.info("📭  No predictions logged yet. Run a prediction to see it here.")
    else:
        h_df = pd.DataFrame(history)

        c1, c2, c3 = st.columns(3)
        with c1:
            st.markdown(f'<div class="lab-card"><h3>Total Predictions</h3><div class="lab-metric">{len(h_df)}</div></div>', unsafe_allow_html=True)
        with c2:
            toxic_n = (h_df["Prediction"] == "Toxic").sum()
            st.markdown(f'<div class="lab-card"><h3>Toxic</h3><div class="lab-metric" style="-webkit-text-fill-color:#ff6b7a; background:none; color:#ff6b7a;">{toxic_n}</div></div>', unsafe_allow_html=True)
        with c3:
            safe_n = (h_df["Prediction"] == "Non-Toxic").sum()
            st.markdown(f'<div class="lab-card"><h3>Non-Toxic</h3><div class="lab-metric" style="-webkit-text-fill-color:#34d399; background:none; color:#34d399;">{safe_n}</div></div>', unsafe_allow_html=True)

        st.dataframe(h_df, use_container_width=True, hide_index=True)

        col_a, col_b = st.columns([1, 1])
        with col_a:
            if st.button("🗑  Clear History"):
                st.session_state["history"] = []
                st.rerun()
        with col_b:
            csv = h_df.to_csv(index=False).encode('utf-8')
            st.download_button("⬇  Export History CSV", csv, "predictions.csv", "text/csv")

        st.caption("ℹ️ For a full per-material PDF report, run a prediction in the Prediction tab — the report download appears alongside the result.")

# =========================================================
# FOOTER
# =========================================================
st.markdown("""
<div class="footer-note">
    🧬 <strong>Reminder:</strong> AI predictions are probabilistic estimates — not laboratory-certified results.
    Always confirm toxicity findings through accredited scientific laboratories,
    in-vitro/in-vivo assays, or peer-reviewed studies before any real-world application.
</div>
""", unsafe_allow_html=True)
